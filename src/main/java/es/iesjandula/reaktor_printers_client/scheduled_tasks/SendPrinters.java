package es.iesjandula.reaktor_printers_client.scheduled_tasks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.iesjandula.base.base_server.security.service.AuthorizationService;
import es.iesjandula.base.base_server.utils.BaseServerException;
import es.iesjandula.reaktor_printers_client.dto.DtoPrinter;
import es.iesjandula.reaktor_printers_client.utils.PrinterClientException;
import es.iesjandula.reaktor_printers_client.utils.PrinterInfoService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Francisco Manuel Benítez Chico
 */
@Slf4j
@Component
public class SendPrinters
{
	@Value("${reaktor.printers_server_url}")
	private String printersServerUrl ;
	
	@Value("${reaktor.bannedPrinters}")
	private String[] bannedPrinters ;
	
	@Autowired
	private PrinterInfoService printerInfoService ;
	
	@Autowired
	private AuthorizationService authorizationService ;

	/**
	 * Metodo encargado de enviar la informacion de las impresoras
	 */
	@Scheduled(fixedDelayString = "${reaktor.fixedDelayString.sendPrinters}")
	public void sendPrinters()
	{
		List<DtoPrinter> listDtoPrinters = new ArrayList<DtoPrinter>() ;
		
		log.info("Se van a localizar la lista de impresoras") ;

		// Pedimos la lista de todas las impresoras
		PrintService[] printServices     = PrintServiceLookup.lookupPrintServices(null, null) ;
		
		// Introducimos aquí las banneadas
		List<String> bannedPrinters      = Arrays.asList(this.bannedPrinters) ;
		
		CloseableHttpClient httpClient   = HttpClients.createDefault() ;
		
		try
		{
			// Iteramos sobre todas las impresoras encontradas
			for (PrintService printer : printServices)
			{
				// Si la impresora no está baneada, la procesamos
				if (!bannedPrinters.contains(printer.getName()))
				{
					try
					{
						log.debug("Se van a obtener información de la impresora {}", printer.getName()) ;
						
						// Obtenemos información de la impresora y la añadimos la impresora a la lista con los datos leídos
						DtoPrinter dtoPrinter = this.printerInfoService.obtenerInfoImpresora(printer) ;
						
						listDtoPrinters.add(dtoPrinter) ;
						
						log.debug("Se ha obtenido información de la impresora {} con estos datos: {}", printer.getName(), dtoPrinter) ;
					}
					catch (PrinterClientException printerClientException)
					{
						// Ya logueado, lo malo que no se puede obtener información de esta impresora
					}
				}
			}

			// Enviamos la petición POST
			this.enviarPeticionPost(httpClient, listDtoPrinters) ;
		} 
		finally
		{
			if (httpClient != null)
			{
				try
				{
					httpClient.close() ;
				}
				catch (IOException ioException)
				{
					log.error("IOException en httpClient mientras se cerraba el flujo de datos", ioException) ;
				}
			}
		}
	}

	/**
	 * @param httpClient HTTP Client
	 * @param listDtoPrinters lista de DTO printers
	 */
	private void enviarPeticionPost(CloseableHttpClient httpClient, List<DtoPrinter> listDtoPrinters)
	{
		try
		{
			log.debug("Se va a preparar la petición POST para actualizar el estado de las impresoras") ;
			
			// Asegúrate de que tu ObjectMapper esté correctamente configurado
			ObjectMapper objectMapper = new ObjectMapper() ;
			
			// Registrar automáticamente cualquier módulo de Jackson necesario
			objectMapper.findAndRegisterModules(); 
	
			// Configuración del HTTP POST con codificación UTF-8
			HttpPost httpPost = new HttpPost(this.printersServerUrl + "/printers/client/printers") ;
			
			// Añadimos el token a la llamada
			httpPost.addHeader("Authorization", "Bearer " + this.authorizationService.obtenerTokenPersonalizado()) ;
			
			// Indicamos que viaja un JSON
			httpPost.setHeader("Content-type", "application/json") ;
	
			// Serialización de la entidad JSON asegurando UTF-8
			StringEntity entity = new StringEntity(objectMapper.writeValueAsString(listDtoPrinters), StandardCharsets.UTF_8) ;
			httpPost.setEntity(entity) ;
			
			log.debug("Se va a enviar la petición POST para actualizar el estado de las impresoras") ;
	
			// Enviamos la petición
			httpClient.execute(httpPost) ;
			
			log.info("Se ha enviado correctamente la petición POST para actualizar el estado de las impresoras") ;
		}
		catch (IOException ioException)
		{
			log.error("IOException mientras se enviaba la petición POST con el estado de las impresoras", ioException) ;
		}
		catch (BaseServerException baseServerException)
		{
			// Excepción logueada previamente
		}
	}
}
