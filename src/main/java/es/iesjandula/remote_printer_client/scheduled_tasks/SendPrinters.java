package es.iesjandula.remote_printer_client.scheduled_tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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

//	private String serverUrl = "http://192.168.1.215:8081/";
	private String serverUrl = "http://localhost:8082/";

	@Scheduled(fixedDelayString = "200", initialDelay = 100)
	public void sendPrinters()
	{
		CloseableHttpClient httpClient = null;
		InputStream inputStream = null;
		BufferedReader reader = null;
		// GETTING HTTP CLIENT
		httpClient = HttpClients.createDefault();
		List<Printer> listPrinters = new ArrayList<Printer>();
		PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
		try
		{
			for (PrintService printer : printServices)
			{
				Process process = Runtime.getRuntime().exec("cmd.exe /c ConsoleApp1.exe \"" +printer.getName() + "\"");
				
				inputStream = process.getInputStream();
				String output = new String(inputStream.readAllBytes());
				
				
				Scanner sc = new Scanner(output);
				
				listPrinters.add(new Printer(printer.getName(), Integer.valueOf(sc.nextLine()), sc.nextLine(), Integer.valueOf(sc.nextLine())));
				
				sc.close();
			}

			HttpPost requestPost = new HttpPost(this.serverUrl + "/send/printers");
			requestPost.setHeader("Content-type", "application/json");

			StringEntity entity;

			entity = new StringEntity(new ObjectMapper().writeValueAsString(listPrinters));
			requestPost.setEntity(entity);

			// --- EJECUTAMOS LLAMADA ---
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
