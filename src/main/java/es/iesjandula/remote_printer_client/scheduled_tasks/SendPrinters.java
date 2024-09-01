package es.iesjandula.remote_printer_client.scheduled_tasks;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.iesjandula.remote_printer_client.dto.DtoPrinter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SendPrinters
{
	@Value("${printer.server.url}")
	private String serverUrl ;
	
	@Value("${printer.banned}")
	private String[] banned ;

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
					this.obtenerInfoImpresora(listDtoPrinters, printer) ;
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
	 * @param listDtoPrinters lista actual de impresoras DTO
	 * @param printer nueva impresora a consultar
	 */
	private void obtenerInfoImpresora(List<DtoPrinter> listDtoPrinters, PrintService printer)
	{
		Process process 		= null ;
		InputStream inputStream = null ;
		Scanner scanner 		= null ; 
		
		try
		{
			// Lanzamos el proceso para que nos informe diciéndole que nos pase la información con tildes (primer comando)
			process 	= Runtime.getRuntime().exec("cmd.exe /c chcp 65001 && ConsoleApp1.exe \"" + printer.getName() + "\"") ;
			
			// Obtenemos el flujo de entrada
			inputStream = process.getInputStream() ;
			
			// Leemos con el scanner
			scanner 	= new Scanner(inputStream, "UTF-8");

			// Ignoramos la primera línea que es el mensaje de cambio de página de códigos
			if (scanner.hasNextLine())
			{
			    scanner.nextLine() ; // Ignoramos la línea de "Página de códigos activa: 65001"
			}

			// Añadimos la impresora a la lista con los datos leídos
			listDtoPrinters.add(new DtoPrinter(printer.getName(), Integer.valueOf(scanner.nextLine()), scanner.nextLine(), Integer.valueOf(scanner.nextLine())));
		}
		catch (IOException ioException)
		{
			log.error("IOException mientras se obtenía información de la impresora " + printer.getName(), ioException) ;
		}
		finally
		{
			if (inputStream != null)
			{
				try
				{
					inputStream.close() ;
				}
				catch (IOException ioException)
				{
					log.error("IOException en inputStream mientras se cerraba el flujo de datos", ioException) ;
				}
			}
			
			if (scanner != null)
			{
				scanner.close() ;
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
