package es.iesjandula.remote_printer_client.scheduled_tasks;

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

import es.iesjandula.remote_printer_client.dto.DtoPrinter;
import es.iesjandula.remote_printer_client.utils.PrinterClientException;
import es.iesjandula.remote_printer_client.utils.PrinterInfoService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SendPrinters
{
	@Value("${printer.server.url}")
	private String serverUrl ;
	
	@Value("${printer.banned}")
	private String[] banned ;
	
	@Autowired
	private PrinterInfoService printerInfoService ;

	/**
	 * Metodo encargado de enviar la informacion de las impresoras
	 */
	@Scheduled(fixedDelayString = "${printer.sendPrinters.fixedDelayString}")
	public void sendPrinters()
	{
		List<DtoPrinter> listDtoPrinters = new ArrayList<DtoPrinter>() ;

		// Pedimos la lista de todas las impresoras
		PrintService[] printServices     = PrintServiceLookup.lookupPrintServices(null, null) ;
		
		// Introducimos aquí las banneadas
		List<String> printersBanned      = Arrays.asList(this.banned) ;
		
		CloseableHttpClient httpClient   = HttpClients.createDefault() ;
		
		try
		{
			// Iteramos sobre todas las impresoras encontradas
			for (PrintService printer : printServices)
			{
				// Si la impresora no está baneada, la procesamos
				if(!printersBanned.contains(printer.getName()))
				{
					try
					{
						// Obtenemos información de la impresora y la añadimos la impresora a la lista con los datos leídos
						listDtoPrinters.add(this.printerInfoService.obtenerInfoImpresora(printer)) ;
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
		catch (IOException ioException)
		{
			log.error("IOException mientras se trataba de enviar el estado de las impresoras", ioException) ;
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
	 * @throws IOException con un error mientras se enviaba la petición POST con el estado de las impresoras
	 */
	private void enviarPeticionPost(CloseableHttpClient httpClient, List<DtoPrinter> listDtoPrinters) throws IOException
	{
		try
		{
			// Asegúrate de que tu ObjectMapper esté correctamente configurado
			ObjectMapper objectMapper = new ObjectMapper() ;
			
			// Registrar automáticamente cualquier módulo de Jackson necesario
			objectMapper.findAndRegisterModules(); 
	
			// Configuración del HTTP POST con codificación UTF-8
			HttpPost requestPost = new HttpPost(this.serverUrl + "/printers/client/printers") ;
			requestPost.setHeader("Content-type", "application/json") ;
	
			// Serialización de la entidad JSON asegurando UTF-8
			StringEntity entity = new StringEntity(objectMapper.writeValueAsString(listDtoPrinters), StandardCharsets.UTF_8) ;
			requestPost.setEntity(entity) ;
	
			// Enviamos la petición
			httpClient.execute(requestPost) ;
		}
		catch (IOException ioException)
		{
			log.error("IOException mientras se enviaba la petición POST con el estado de las impresoras", ioException) ;
		}
	}
}
