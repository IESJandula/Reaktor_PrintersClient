package es.iesjandula.remote_printer_client.scheduled_tasks;

import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.Sides;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;
import org.apache.pdfbox.printing.PDFPrintable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Print
{

//	private String serverUrl = "http://192.168.1.215:8081/";
	private String serverUrl = "http://localhost:8081/";

	@Scheduled(fixedDelayString = "1000", initialDelay = 2000)
	public void print()
	{
		log.info("intento de imprimir");
		CloseableHttpClient httpClient = null;
		CloseableHttpResponse response = null;
		InputStream inputStream = null;
		// GETTING HTTP CLIENT
		httpClient = HttpClients.createDefault();

		HttpGet request = new HttpGet(this.serverUrl + "/get/prints");
		try
		{
			response = httpClient.execute(request);

			if (response.containsHeader("Content-Disposition"))
			{
				inputStream = response.getEntity().getContent();

				String numCopies = response.getFirstHeader("numCopies").getValue();
				String printerName = response.getFirstHeader("printerName").getValue();
				String color = response.getFirstHeader("color").getValue();
				String vertical = response.getFirstHeader("orientation").getValue();
				String faces = response.getFirstHeader("faces").getValue();

				this.printFile(printerName, Integer.valueOf(numCopies), Boolean.valueOf(color),
						Boolean.valueOf(vertical), Boolean.valueOf(faces), inputStream);
			}
		} catch (JsonProcessingException exception)
		{
			String error = "Error Json Processing Exception";
			log.error(error, exception);
		} catch (UnsupportedEncodingException exception)
		{
			String error = "Error Unsupported Encoding Exception";
			log.error(error, exception);
		} catch (ClientProtocolException exception)
		{
			String error = "Error Client Protocol Exception";
			log.error(error, exception);
		} catch (IOException exception)
		{
			String error = "Error In Out Exception";
			log.error(error, exception);
		} finally
		{
			// --- CERRAMOS ---
			this.closeHttpClientResponse(httpClient, response);
		}
	}

	public void printFile(String printerName, int numCopies, boolean color, boolean vertical, boolean faces,
			InputStream input)
	{

		// --- FLUJOS ---
		DataInputStream dataInputStream = null;
		PDDocument pdDocument = null;
		try
		{
			dataInputStream = new DataInputStream(input);

			PrintService selectedPrinter = this.selectPrinter(printerName);
			
			if (selectedPrinter != null)
			{
				PrinterJob printerJob = PrinterJob.getPrinterJob();

				pdDocument = PDDocument.load(input);
				PDFPageable pageable = new PDFPageable(pdDocument);
				PageFormat format = pageable.getPageFormat(0);
				if (vertical)
				{
					format.setOrientation(PageFormat.PORTRAIT);
				} else
				{
					format.setOrientation(PageFormat.LANDSCAPE);
				}

				printerJob.setPrintable(new PDFPrintable(pdDocument), format);
				printerJob.setPrintService(selectedPrinter);
				HashPrintRequestAttributeSet attributeSet = new HashPrintRequestAttributeSet();
				if (color)
				{
					attributeSet.add(Chromaticity.COLOR);
				} else
				{
					attributeSet.add(Chromaticity.MONOCHROME);
				}
				if (faces)
				{
					attributeSet.add(Sides.DUPLEX);
				} else
				{
					attributeSet.add(Sides.ONE_SIDED);
				}
				attributeSet.add(new Copies(numCopies));
				printerJob.print(attributeSet);

			} else
			{
				String error = "Printer erronea";
				log.error(error);
			}

		} catch (IOException exception)
		{
			String error = "Error leyendo el fichero";
			log.error(error, exception);

		} catch (PrinterException exception)
		{
			String error = "Error imprimiendo";
			log.error(error, exception);
		} finally
		{
			this.closePrintInputs(dataInputStream, pdDocument);
		}
	}

	private PrintService selectPrinter(String printerName)
	{
		PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
		PrintService selectedPrinter = null;
		int i = 0;
		while (i < printServices.length && selectedPrinter == null)
		{
			if (printServices[0].getName().equals(printerName))
			{
				selectedPrinter = printServices[0];
			}
			i++;
		}
		return selectedPrinter;
	}

	private void closePrintInputs(DataInputStream dataInputStream, PDDocument pdDocument)
	{
		if (dataInputStream != null)
		{
			try
			{
				dataInputStream.close();
			} catch (IOException exception)
			{
				String message = "Error";
				log.error(message, exception);
			}
		}
		if (pdDocument != null)
		{
			try
			{
				pdDocument.close();
			} catch (IOException exception)
			{
				String message = "Error";
				log.error(message, exception);
			}
		}
	}

	private void closeHttpClientResponse(CloseableHttpClient httpClient, CloseableHttpResponse response)
	{
		if (httpClient != null)
		{
			try
			{
				httpClient.close();
			} catch (IOException exception)
			{
				String error = "Error In Out Exception";
				log.error(error, exception);
			}
		}
		if (response != null)
		{
			try
			{
				response.close();
			} catch (IOException exception)
			{
				String error = "Error In Out Exception";
				log.error(error, exception);
			}
		}
	}

}
