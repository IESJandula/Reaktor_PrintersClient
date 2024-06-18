package es.iesjandula.remote_printer_client.scheduled_tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.iesjandula.remote_printer_client.models.Printer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SendPrinters
{

	@Value("${printer.server.url}")
	private String serverUrl = "http://localhost:8082/";
	
	private List<String> printersBanned = Arrays.asList("Microsoft XPS Document Writer", "Fax", "OneNote for Windows 10", "Send To OneNote 2016", "Microsoft Print to PDF");

	/**
	 * Metodo encargado de enviar la informacion de las impresoras
	 */
	@Scheduled(fixedDelayString = "200", initialDelay = 100)
	public void sendPrinters()
	{
		CloseableHttpClient httpClient = null;
		InputStream inputStream = null;
		BufferedReader reader = null;
		httpClient = HttpClients.createDefault();
		List<Printer> listPrinters = new ArrayList<Printer>();
		//Pide la lista de todas las impresoras
		PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
		try
		{
			//Creacion de los objetos print
			for (PrintService printer : printServices)
			{
				if(!printersBanned.contains(printer.getName()))
				{
					Process process = Runtime.getRuntime().exec("cmd.exe /c ConsoleApp1.exe \"" +printer.getName() + "\"");
					
					inputStream = process.getInputStream();
					String output = new String(inputStream.readAllBytes());
					
					
					Scanner sc = new Scanner(output);
					
					listPrinters.add(new Printer(printer.getName(), Integer.valueOf(sc.nextLine()), sc.nextLine(), Integer.valueOf(sc.nextLine())));
					
					sc.close();
				}
				
			}
			//Envio al servidor de la informacion
			HttpPost requestPost = new HttpPost(this.serverUrl + "/send/printers");
			requestPost.setHeader("Content-type", "application/json");

			StringEntity entity;

			entity = new StringEntity(new ObjectMapper().writeValueAsString(listPrinters));
			requestPost.setEntity(entity);

			httpClient.execute(requestPost);
		} catch (JsonProcessingException e)
		{
			String error = "Error procesando info a json";
			log.error(error, e);
		} catch (UnsupportedEncodingException e)
		{
			String error = "Error unsupported encoding";
			log.error(error, e);
		} catch (ClientProtocolException e)
		{
			String error = "Error sendign printers";
			log.error(error, e);
		} catch (IOException e)
		{
			String error = "Error IO";
			log.error(error, e);
		}finally {
			
			if (httpClient != null)
			{
				try
				{
					httpClient.close();
				} catch (IOException e)
				{
					String error = "Error IO httpClient";
					log.error(error, e);
				}
			}
			
			if (reader != null)
			{
				try
				{
					reader.close();
				} catch (IOException e)
				{
					String error = "Error IO reader";
					log.error(error, e);
				}
			}
			
			if (inputStream != null)
			{
				try
				{
					inputStream.close();
				} catch (IOException e)
				{
					String error = "Error IO inputStream";
					log.error(error, e);
				}
			}
			
			
		}

	}

}
