package es.iesjandula.reaktor_printers_client.scheduled_tasks;

import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.Sides;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import es.iesjandula.base.base_server.security.service.AuthorizationService;
import es.iesjandula.base.base_server.utils.BaseServerException;
import es.iesjandula.reaktor_printers_client.dto.DtoPrintAction;
import es.iesjandula.reaktor_printers_client.dto.DtoPrinter;
import es.iesjandula.reaktor_printers_client.utils.Constants;
import es.iesjandula.reaktor_printers_client.utils.PrinterClientException;
import es.iesjandula.reaktor_printers_client.utils.PrinterInfoService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Francisco Manuel Benítez Chico
 */
@Slf4j
@Component
public class Print
{
	@Value("${reaktor.printers_server_url}")
	private String printersServerUrl ;
	
	@Autowired
	private AuthorizationService authorizationService ;
	
	@Autowired
	private PrinterInfoService printerInfoService ;

	/**
	 * Funcion que cada X tiempo pregunta al servidor si hay que imprimir algo y si lo hay lo imprime
	 */
	@Scheduled(fixedDelayString = "${reaktor.fixedDelayString.print}")
	public void imprimir()
	{
		log.info("INICIO - Comprobación de si hay algo para imprimir") ;
		
		CloseableHttpClient httpClient = HttpClients.createDefault() ;

		DtoPrintAction dtoPrintAction = null ;
		try
		{
			// Buscamos la tarea para imprimir
			dtoPrintAction = this.buscarTareaParaImprimir(httpClient) ;
			
			// Si hay ninguna acción que hacer ...
			if (dtoPrintAction != null)
			{
				// Logueamos
				log.info("Se ha encontrado una tarea para imprimir: {}", dtoPrintAction) ;
				
				// Imprimimos
				this.imprimirInternal(httpClient, dtoPrintAction) ;	
			}
		}
		catch (PrinterClientException | BaseServerException reaktorException)
		{
			// Logueada previamente en el método
		}
		
		try
		{
			// Cerramos el CloseableHttpClient
			httpClient.close() ;
		}
		catch (IOException ioException)
		{
			log.error("Error al cerrar CloseableHttpClient: " + ioException.getMessage(), ioException) ;
		}
		
		log.info("FIN - Comprobación de si hay algo para imprimir") ;
	}

	/**
	 * @param httpClient HTTP Client
	 * @param dtoPrintAction tarea para imprimir
	 * @throws PrinterClientException con un error
	 */
	private void imprimirInternal(CloseableHttpClient httpClient, DtoPrintAction dtoPrintAction)
	{
		try
		{
			// ... imprimimos la tarea
			this.imprimirDocumento(dtoPrintAction) ;
			
			// Enviamos la respuesta al servidor de que todo ha ido bien
			this.enviarRespuestaAlServidor(httpClient, dtoPrintAction, null) ;
			
			// Logueamos
			log.info("Se ha enviado respuesta al servidor de la tarea impresa correctamente: {}", dtoPrintAction) ;
		}
		catch (PrinterClientException printerClientException)
		{
			try
			{
				// Enviamos la respuesta al servidor de que ha habido un error
				this.enviarRespuestaAlServidor(httpClient, dtoPrintAction, printerClientException) ;
				
				// Logueamos
				log.info("Se ha enviado respuesta al servidor de la tarea NO impresa: {}", dtoPrintAction) ;
			}
			catch (PrinterClientException | BaseServerException reaktorException)
			{
				// Logueada previamente en el método
			}			
		}
		catch (BaseServerException e)
		{
			// Logueada previamente en el método
			// No podemos hacer más ya que es un problema con el JWT
		}
	}
	
	/**
	 * @param httpClient HTTP Client
	 * @return tarea para imprimir
	 * @throws PrinterClientException con un error
	 * @throws BaseServerException con un error al obtener el token JWT
	 */
	private DtoPrintAction buscarTareaParaImprimir(CloseableHttpClient httpClient) throws PrinterClientException, BaseServerException
	{
		DtoPrintAction outcome = null ;
		CloseableHttpResponse closeableHttpResponse = null ;
		InputStream contenidoFicheroOriginal = null ;
		
		try
		{
			HttpGet httpGet = new HttpGet(this.printersServerUrl + "/printers/client/print") ;
			
			// Añadimos el token a la llamada
			httpGet.addHeader("Authorization", "Bearer " + this.authorizationService.obtenerTokenPersonalizado()) ;
			
			// Hacemos la peticion
			closeableHttpResponse = httpClient.execute(httpGet) ;
			
			// Comprobamos si viene la cabecera. En caso afirmativo, es porque trae un fichero a imprimir
			if (closeableHttpResponse.containsHeader(Constants.HEADER_PRINT_CONTENT_DISPOSITION))
			{
				// Obtenemos los parametros de la impresion
				String id 	          = closeableHttpResponse.getFirstHeader(Constants.HEADER_PRINT_ID).getValue() ;
				String user    	      = closeableHttpResponse.getFirstHeader(Constants.HEADER_PRINT_USER).getValue() ;
				String printer 	   	  = closeableHttpResponse.getFirstHeader(Constants.HEADER_PRINT_PRINTER).getValue() ;
				Integer copies     	  = Integer.valueOf(closeableHttpResponse.getFirstHeader(Constants.HEADER_PRINT_COPIES).getValue()) ;
				Boolean blackAndWhite = Boolean.valueOf(closeableHttpResponse.getFirstHeader(Constants.HEADER_PRINT_COLOR).getValue()) ;
				Boolean vertical 	  = Boolean.valueOf(closeableHttpResponse.getFirstHeader(Constants.HEADER_PRINT_ORIENTATION).getValue()) ;
				Boolean twoSides 	  = Boolean.valueOf(closeableHttpResponse.getFirstHeader(Constants.HEADER_PRINT_SIDES).getValue()) ;
			
				// Creamos una nueva instancia de DtoPrintAction
				outcome = new DtoPrintAction() ;
				
				// Asignamos los valores
				outcome.setId(id) ;
				outcome.setUser(user) ;
				outcome.setPrinter(printer) ;
				outcome.setCopies(Integer.valueOf(copies)) ;
				outcome.setBlackAndWhite(blackAndWhite) ;
				outcome.setVertical(vertical) ;
				outcome.setTwoSides(twoSides) ;
				
				// Obtenemos el contenido del documento a imprimir
				contenidoFicheroOriginal = closeableHttpResponse.getEntity().getContent() ;
				
				// Copiamos el contenido del InputStream original en nuestro objeto outcome
				this.copiarContenidoInputStreamOriginal(outcome, contenidoFicheroOriginal) ;				
			}
		}
		catch (IOException ioException)
		{
			String errorString = "IOException mientras se buscaba la tarea para imprimir en el servidor" ;
			
			log.error(errorString, ioException) ;
			throw new PrinterClientException(errorString, ioException) ;
		}
		finally
		{
			// Cierre de flujos
			this.buscarTareaParaImprimirCierreFlujos(closeableHttpResponse) ;
		}
		
		return outcome ;
	}

	/**
	 * @param outcome outcome
	 * @param contenidoFicheroOriginal contenido fichero original
	 * @throws PrinterClientException con un error
	 */
	private void copiarContenidoInputStreamOriginal(DtoPrintAction outcome, InputStream contenidoFicheroOriginal) throws PrinterClientException
	{
		ByteArrayOutputStream buffer = new ByteArrayOutputStream() ;

		try
		{
	        // Transferimos directamente el contenido del InputStream al ByteArrayOutputStream
	        contenidoFicheroOriginal.transferTo(buffer) ;
	        
	        // Convertimos el contenido copiado a un ByteArrayInputStream
	        ByteArrayInputStream contenidoFichero = new ByteArrayInputStream(buffer.toByteArray());
	        
	        // Guardamos en el objeto outcome dentro del contenido del fichero
	        outcome.setContenidoFichero(contenidoFichero);
		}
		catch (IOException ioException)
		{
			String errorString = "IOException mientras se copiaba el inputStream 'contenidoFicheroOriginal' en nuestro objeto interno" ;
			
			log.error(errorString, ioException) ;
			throw new PrinterClientException(errorString, ioException) ;			
		}
		finally
		{
			if (contenidoFicheroOriginal != null)
			{
				try
				{
					// Cerramos el InputStream original ya que ya copiamos el contenido
					contenidoFicheroOriginal.close() ;
				}
				catch (IOException ioException)
				{
					String errorString = "IOException mientras se cerraba el inputStream 'contenidoFicheroOriginal' en nuestro objeto interno" ;
					
					log.error(errorString, ioException) ;
					throw new PrinterClientException(errorString, ioException) ;	
				}
			}
			
			if (buffer != null)
			{
				try
				{
					// Cerramos el buffer ya que ya copiamos el contenido
					buffer.close() ;
				}
				catch (IOException ioException)
				{
					String errorString = "IOException mientras se cerraba el buffer 'contenidoFicheroOriginal' en nuestro objeto interno" ;
					
					log.error(errorString, ioException) ;
					throw new PrinterClientException(errorString, ioException) ;	
				}				
			}
		}
	}

	/**
	 * @param closeableHttpResponse closeable HTTP response
	 * @throws PrinterClientException printer client exception
	 */
	private void buscarTareaParaImprimirCierreFlujos(CloseableHttpResponse closeableHttpResponse) throws PrinterClientException
	{
		if (closeableHttpResponse != null)
		{
			try
			{
				closeableHttpResponse.close() ;
			}
			catch (IOException ioException)
			{
				String errorString = "IOException mientras se cerraba el closeableHttpResponse en el método que busca la tarea para imprimir en el servidor" ;
				
				log.error(errorString, ioException) ;
				throw new PrinterClientException(errorString, ioException) ;
			}
		}
	}

	/**
	 * Imprime un documento con la configuración pasada como parametros
	 * @param dtoPrintAction tarea a imprimir
	 * @throws PrinterClientException con un error
	 */
	private void imprimirDocumento(DtoPrintAction dtoPrintAction) throws PrinterClientException
	{
	    // Flujos
	    PDDocument pdDocument = null;

	    try
	    {
	        // Buscamos la impresora
	        PrintService selectedPrinter = this.buscarImpresora(dtoPrintAction);

	        // Validamos el estado de la impresora
	        this.validarEstadoImpresora(selectedPrinter);

	        // Creamos el JOB de impresión
	        PrinterJob printerJob = PrinterJob.getPrinterJob();

	        // Seleccionamos la impresora
	        printerJob.setPrintService(selectedPrinter);

  	        // Obtenemos el contenido del documento en pdfBytes
	        byte[] pdfBytes = dtoPrintAction.getContenidoFichero().readAllBytes() ;
	        
	        // Introducimos el contenido del documento
			pdDocument = Loader.loadPDF(pdfBytes) ;

	        // Configurar e imprimir documento
	        this.configurarEimprimirDocumento(dtoPrintAction, pdDocument, printerJob);
	    }
	    catch (PrinterException printerException)
	    {
	        String errorString = "PrinterException mientras se imprimía el documento";
	        log.error(errorString, printerException);
	        throw new PrinterClientException(errorString, printerException);
	    }
	    catch (IOException ioException)
	    {
	        String errorString = "IOException mientras se introducía el contenido del fichero a imprimir";
	        log.error(errorString, ioException);
	        throw new PrinterClientException(errorString, ioException);
	    }
	    finally
	    {
	        // Cerramos los flujos
	        this.imprimirCierreFlujos(pdDocument, dtoPrintAction.getContenidoFichero());
	    }
	}

	/**
	 * @param dtoPrintAction DTO Print Action
	 * @return el servicio de impresora encontrada
	 * @throws PrinterClientException con un error
	 */
	private PrintService buscarImpresora(DtoPrintAction dtoPrintAction) throws PrinterClientException
	{
		// Elegimos la impresora
		PrintService selectedPrinter = this.buscarImpresoraInternal(dtoPrintAction.getPrinter()) ;
		
		// Si no está en la lista mandamos una excepción
		if (selectedPrinter == null)
		{
			String errorString = "La impresora " + dtoPrintAction.getPrinter() + " no está en la lista" ;
			
			log.error(errorString) ;
			throw new PrinterClientException(errorString) ;				
		}
		
		return selectedPrinter ;
	}
	
	/**
	 * Metodo que selecciona la impresora
	 * @param printer nombre de la impresora
	 * @return el servicio de impresora
	 */
	private PrintService buscarImpresoraInternal(String printer)
	{
		PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null) ;
		PrintService selectedPrinter = null ;
		
		int i = 0;
		while (i < printServices.length && selectedPrinter == null)
		{
			if (printServices[i].getName().equals(printer))
			{
				selectedPrinter = printServices[i] ;
			}
			
			i++ ;
		}
		
		return selectedPrinter ;
	}
	
	/**
	 * @param selectedPrinter impresora seleccionada
	 * @throws PrinterClientException con un error
	 */
	private void validarEstadoImpresora(PrintService selectedPrinter) throws PrinterClientException
	{
		// Obtenemos información de la impresora
		DtoPrinter dtoPrinter = this.printerInfoService.obtenerInfoImpresora(selectedPrinter) ;
		
		// Validamos el estado de la impresora
		if (dtoPrinter.getStatusId() != 0)
		{
			log.error("Error en " + selectedPrinter + ": " + dtoPrinter.getStatus()) ;
			throw new PrinterClientException(dtoPrinter.getStatus()) ;				
		}
	}
	
	/**
	 * @param dtoPrintAction DTO Print Action
	 * @param pdDocument PD Document
	 * @param printerJob Printer JOB
	 * @throws PrinterException con un error
	 */
	private void configurarEimprimirDocumento(DtoPrintAction dtoPrintAction, PDDocument pdDocument, PrinterJob printerJob) throws PrinterException
	{
	    // Configuramos color, caras y copias
	    HashPrintRequestAttributeSet attributeSetDocumentoPrincipal = this.configurarColorCarasYcopias(dtoPrintAction, printerJob) ;

	    // Configuramos la orientación (actualizado)
	    this.configurarOrientacion(dtoPrintAction, pdDocument, printerJob, attributeSetDocumentoPrincipal) ;
	    
	    // Logueamos
	    log.info("IMPRESION - Se va a enviar a la cola de impresión") ;

	    // Imprimimos el documento principal
	    printerJob.print(attributeSetDocumentoPrincipal) ;
	    
	    // Logueamos
	    log.info("IMPRESION - Se ha realizado la impresión") ;
	}
	
	/**
	 * @param dtoPrintAction DTO Print Action
	 * @param pdDocument PD Document
	 * @param printerJob printer job
	 * @param attributeSetDocumentoPrincipal attribute set documento principal
	 */
	private void configurarOrientacion(DtoPrintAction dtoPrintAction,
									   PDDocument pdDocument,
									   PrinterJob printerJob,
									   HashPrintRequestAttributeSet attributeSetDocumentoPrincipal)
	{
	    // Ajustamos la orientación según el documento
	    if (dtoPrintAction.getVertical())
	    {
			PDFPageable pageable = new PDFPageable(pdDocument) ;
			PageFormat format = pageable.getPageFormat(0) ;
			
			format.setOrientation(PageFormat.PORTRAIT) ;
	    	
			// La hacemos printable
			printerJob.setPrintable(new PDFPrintable(pdDocument), format) ;
	    }
	    else
	    {
	    	// Horizontal
	        attributeSetDocumentoPrincipal.add(OrientationRequested.LANDSCAPE) ;
	        
		    // Establecemos el printable al printerJob adaptándola al espacio
		    printerJob.setPrintable(new PDFPrintable(pdDocument, Scaling.SHRINK_TO_FIT)) ;
	    }
	}
	
	/**
	 * @param dtoPrintAction DTO Print Action
	 * @param printerJob printer job
	 * @return mapa con los atributos de la página a imprirmir configurados
	 */
	private HashPrintRequestAttributeSet configurarColorCarasYcopias(DtoPrintAction dtoPrintAction, PrinterJob printerJob)
	{
		HashPrintRequestAttributeSet outcome = new HashPrintRequestAttributeSet();
		
		// Configuramos color
		if (dtoPrintAction.getBlackAndWhite())
		{
			outcome.add(Chromaticity.MONOCHROME);
		} 
		else
		{
			outcome.add(Chromaticity.COLOR);			
		}
		
		// Configuramos caras
		if (dtoPrintAction.getTwoSides())
		{
			outcome.add(Sides.DUPLEX);
		} 
		else
		{
			outcome.add(Sides.ONE_SIDED);
		}
		
		// Configuramos copias
		outcome.add(new Copies(dtoPrintAction.getCopies())) ;
		
		return outcome ;
	}
	
	/**
	 * Metodo encargado de cerrar todos los flujos utilizados en el metodo print
	 * @param pdDocument PD Document
	 * @param contenidoFichero contenido del fichero
	 * @throws PrinterClientException con un error
	 */
	private void imprimirCierreFlujos(PDDocument pdDocument, InputStream contenidoFichero) throws PrinterClientException
	{
		if (pdDocument != null)
		{
			try
			{
				pdDocument.close();
			}
			catch (IOException ioException)
			{
				String errorString = "IOException mientras se cerraba PD Document en el cierre de flujos de imprimir" ;
				
				log.error(errorString, ioException) ;
				throw new PrinterClientException(errorString, ioException) ;
			}
		}

		if (contenidoFichero != null)
		{
			try
			{
				contenidoFichero.close() ;
			}
			catch (IOException ioException)
			{
				String errorString = "IOException mientras se cerraba el contenido del fichero en el cierre de flujos de imprimir" ;
				
				log.error(errorString, ioException) ;
				throw new PrinterClientException(errorString, ioException) ;
			}
		}
	}
	
	/**
	 * @param httpClient HTTP Client
	 * @param dtoPrintAction DTO Print Action
	 * @param printerClientException printer client Exception
	 * @throws PrinterClientException error al enviar la respuesta
	 * @throws BaseServerException con un error al obtener el token JWT
	 */
	private void enviarRespuestaAlServidor(CloseableHttpClient httpClient,
										   DtoPrintAction dtoPrintAction,
										   PrinterClientException printerClientException) throws PrinterClientException, BaseServerException
	{
		// Devolvemos el resultado al servidor
		HttpPost postRequest = new HttpPost(this.printersServerUrl + "/printers/client/status") ;
		
		// Añadimos el id de la tarea para que la actualice
		postRequest.addHeader(Constants.HEADER_PRINT_ID, dtoPrintAction.getId()) ;
		
		// Añadimos el token a la llamada
		postRequest.addHeader("Authorization", "Bearer " + this.authorizationService.obtenerTokenPersonalizado()) ;
		
		if (printerClientException == null)
		{
			postRequest.addHeader(Constants.RESPONSE_SERVER_KEY_STATUS, Constants.STATE_DONE) ;
		}
		else
		{
			postRequest.addHeader(Constants.RESPONSE_SERVER_KEY_STATUS, Constants.STATE_ERROR) ;
			postRequest.addHeader(Constants.RESPONSE_SERVER_KEY_MESSAGE, printerClientException.getMessage()) ;
			postRequest.addHeader(Constants.RESPONSE_SERVER_KEY_EXCEPTION, printerClientException.getException()) ;
		}
		
		try
		{
			// Enviamos la respuesta al cliente en una request
			httpClient.execute(postRequest) ;
		}
		catch (IOException ioException)
		{
			String errorString = "IOException mientras se enviaba la respuesta al servidor" ;
			
			log.error(errorString, ioException) ;
			throw new PrinterClientException(errorString, ioException) ;
		}
	}
}

