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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.printing.PDFPageable;
import org.apache.pdfbox.printing.PDFPrintable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import es.iesjandula.remote_printer_client.error.PrinterError;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Print
{

	@Value("${printer.server.url}")
	private String serverUrl ;

	/**
	 * Funcion que cada segundo pregunta al servidor si hay que imprimir algo y si lo hay lo imprime
	 */
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
		HttpPost postRequest = new HttpPost(this.serverUrl + "/get/print/status");
		String id = "";
		try
		{
			//Hace la peticion
			response = httpClient.execute(request);
			if (response.containsHeader("Content-Disposition"))
			{
				inputStream = response.getEntity().getContent();
				
				//Saca los parametros de la impresion
				String numCopies = response.getFirstHeader("numCopies").getValue();
				String printerName = response.getFirstHeader("printerName").getValue();
				String color = response.getFirstHeader("color").getValue();
				String vertical = response.getFirstHeader("orientation").getValue();
				String faces = response.getFirstHeader("faces").getValue();
				String user = response.getFirstHeader("user").getValue();
				id = response.getFirstHeader("id").getValue();
				postRequest.addHeader("id", id);
				try
				{
					//Imprime
					this.printFile(printerName, Integer.valueOf(numCopies), Boolean.valueOf(color),
							Boolean.valueOf(vertical), Boolean.valueOf(faces), user, inputStream);
					postRequest.addHeader("status", "done");
					httpClient.execute(postRequest);
				} catch (PrinterError e)
				{
					postRequest.addHeader("status", "error");
					httpClient.execute(postRequest);
				}
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

	/**
	 *  Imprime un documento con la configuración pasada como parametros
	 * @param printerName
	 * @param numCopies
	 * @param color
	 * @param vertical
	 * @param faces
	 * @param user
	 * @param input
	 * @throws PrinterError
	 */
	public void printFile(String printerName, int numCopies, boolean color, boolean vertical, boolean faces, String user,
			InputStream input) throws PrinterError
	{

		//Flujos
		DataInputStream dataInputStream = null;
		PDDocument pdDocument = null;
		PDDocument pdPageExtra = null;
		PDPageContentStream contentStream = null;
		try
		{
			dataInputStream = new DataInputStream(input);

			//Elegir la impresora
			PrintService selectedPrinter = this.selectPrinter(printerName);
			
			if (selectedPrinter != null)
			{
				PrinterJob printerJob = PrinterJob.getPrinterJob();
				
				//Introducir el contenido del documento
				pdDocument = PDDocument.load(input);
				//Creacion de pagina con el nombre del usuario
				pdPageExtra = new PDDocument();
				PDPage newPage = new PDPage();
				pdPageExtra.addPage(newPage);

				contentStream = new PDPageContentStream(pdDocument, newPage);
				
				//Introduccion del texto de la pagina del usuario
				contentStream.setFont(PDType1Font.HELVETICA_BOLD, 50);
				contentStream.beginText();
				contentStream.newLineAtOffset(100, 700);
				contentStream.showText(user);
				contentStream.endText();
				contentStream.close();

				//Configuracion de la impresion
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
				
				//Impresion del documento
				printerJob.print(attributeSet);
				HashPrintRequestAttributeSet attributeSet2 = new HashPrintRequestAttributeSet();
				attributeSet2.add(new Copies(1));
				
				//Impresion de la pagina del usuario
				printerJob.setPrintable(new PDFPrintable(pdPageExtra));
				printerJob.print(attributeSet2);

			} else
			{
				String error = "Printer erronea";
				log.error(error);
				throw new PrinterError(error);
			}

		} catch (IOException exception)
		{
			String error = "Error leyendo el fichero";
			log.error(error, exception);
			throw new PrinterError(error, exception);

		} catch (PrinterException exception)
		{
			String error = "Error imprimiendo";
			log.error(error, exception);
			throw new PrinterError(error, exception);
		} finally
		{
			this.closePrintInputs(dataInputStream, pdDocument, pdPageExtra, contentStream);
		}
	}

	/**
	 * Metodo que selecciona la impresora
	 * @param printerName
	 * @return
	 */
	private PrintService selectPrinter(String printerName)
	{
		PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
		PrintService selectedPrinter = null;
		int i = 0;
		while (i < printServices.length && selectedPrinter == null)
		{
			if (printServices[i].getName().equals(printerName))
			{
				selectedPrinter = printServices[i];
			}
			i++;
		}
		return selectedPrinter;
	}

	/**
	 *  Metodo encargado de cerrar todos los flujos utilizados en el metodo print
	 * @param dataInputStream
	 * @param pdDocument
	 * @param pdPageExtra
	 * @param contentStream
	 */
	private void closePrintInputs(DataInputStream dataInputStream, PDDocument pdDocument,PDDocument pdPageExtra ,PDPageContentStream contentStream)
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
		if (pdPageExtra != null)
		{
			try
			{
				pdPageExtra.close();
			} catch (IOException exception)
			{
				String message = "Error";
				log.error(message, exception);
			}
		}
		if (contentStream != null)
		{
			try
			{
				contentStream.close();
			} catch (IOException exception)
			{
				String message = "Error";
				log.error(message, exception);
			}
		}
	}

	/**
	 * Metodo encargado de cerrar todos los flujos utilizados la peticion
	 * @param httpClient
	 * @param response
	 */
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
