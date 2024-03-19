package es.iesjandula.remote_printer_client.scheduled_tasks;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SendPrinters
{

//	private String serverUrl = "http://192.168.1.215:8081/";
	private String serverUrl = "http://localhost:8081/";

	@Scheduled(fixedDelayString = "200", initialDelay = 100)
	public void sendPrinters()
	{
		CloseableHttpClient httpClient = null;
		// GETTING HTTP CLIENT
		httpClient = HttpClients.createDefault();
		List<String> listPrinters = new ArrayList<String>();
		PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);

		for (PrintService printer : printServices)
		{
			listPrinters.add(printer.getName());
		}

		HttpPost requestPost = new HttpPost(this.serverUrl + "/send/printers");
		requestPost.setHeader("Content-type", "application/json");
		
		StringEntity entity;
		try
		{
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
		}

	}

}
