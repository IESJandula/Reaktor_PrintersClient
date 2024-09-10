package es.iesjandula.reaktor_printers_client.scheduled_tasks;

import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.Sides;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.printing.PDFPageable;
import org.apache.pdfbox.printing.PDFPrintable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import es.iesjandula.base.base_server.firebase.AuthorizationService;
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
		
		byte[] data = new byte[1024] ;
		
		try
		{
			// Leemos para ver si hemos llegado al final
			int nRead = contenidoFicheroOriginal.read(data, 0, data.length) ;
			
			while (nRead != -1)
			{
				// Escribimos en el buffer
			    buffer.write(data, 0, nRead) ;

				// Leemos para ver si hemos llegado al final
			    nRead = contenidoFicheroOriginal.read(data, 0, data.length) ;
			}
	
			// Convertimos el contenido copiado a un ByteArrayInputStream (lo cerraremos al final)
			ByteArrayInputStream contenidoFichero = new ByteArrayInputStream(buffer.toByteArray()) ;
			
			// Guardamos en el objeto outcome dentro del contenido del fichero
			outcome.setContenidoFichero(contenidoFichero) ;
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
	public void imprimirDocumento(DtoPrintAction dtoPrintAction) throws PrinterClientException
	{
		// Flujos
		PDDocument pdDocument  = null ;
		PDDocument pdPageExtra = null ;
		
		try
		{
			// Buscamos la impresora
			PrintService selectedPrinter = this.buscarImpresora(dtoPrintAction) ;
			
			// Validamos el estado de la impresora
			this.validarEstadoImpresora(selectedPrinter) ;
			
			// Creamos el JOB de impresión
			PrinterJob printerJob = PrinterJob.getPrinterJob() ;
			
			// Seleccionamos la impresora
			printerJob.setPrintService(selectedPrinter) ;
			
			// Introducimos el contenido del documento
			pdDocument = PDDocument.load(dtoPrintAction.getContenidoFichero());
			
			// Creamos una página con el nombre del usuario
			pdPageExtra = this.crearPaginaNombreUsuario(dtoPrintAction, pdDocument) ;
			
			// Configurar e imprimir documento
			this.configurarEimprimirDocumento(dtoPrintAction, pdDocument, printerJob) ;
			
			// Configurar e imprimir página del usuario
			this.configurarEimprimirPaginaUsuario(dtoPrintAction, pdPageExtra, printerJob) ;
		}
		catch (PrinterException printerException)
		{
			String errorString = "PrinterException mientras se imprimía el documento" ;
			
			log.error(errorString, printerException) ;
			throw new PrinterClientException(errorString, printerException) ;
		}
		catch (IOException ioException)
		{
			String errorString = "IOException mientras se introducía el contenido del fichero a imprimir" ;
			
			log.error(errorString, ioException) ;
			throw new PrinterClientException(errorString, ioException) ;
		}
		finally
		{
			// Cerramos los flujos
			this.imprimirCierreFlujos(pdDocument, pdPageExtra, dtoPrintAction.getContenidoFichero()) ;
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
	 * @return PD Page extra
	 * @throws PrinterClientException con un error
	 */
	private PDDocument crearPaginaNombreUsuario(DtoPrintAction dtoPrintAction, PDDocument pdDocument) throws PrinterClientException
	{
	    // Crea un nuevo documento PDF que será el resultado
	    PDDocument outcome = new PDDocument() ;
	    
	    // Crea una nueva página para añadir al documento
	    PDPage newPage = new PDPage() ;
	    
	    // Añade la nueva página al documento de resultado
	    outcome.addPage(newPage) ;

		PDPageContentStream contentStream = null ; 
		
		try 
		{
			// Inicia un flujo de contenido para la nueva página creada
	        contentStream = new PDPageContentStream(outcome, newPage) ;

	        // Configura el estilo del texto con la fuente y el tamaño de fuente
	        float fontSize = 50 ;
	        contentStream.setFont(PDType1Font.HELVETICA_BOLD, fontSize) ;
	        float leading = 1.5f * fontSize ; // Espacio entre líneas del texto

	        // Obtiene el nombre del usuario a escribir en la página
	        String userName = dtoPrintAction.getUser() ;
	        
	        float margin = 50 ; // Define el margen alrededor del texto
	        float width = newPage.getMediaBox().getWidth() - 2 * margin ; // Calcula el ancho disponible para el texto
	        float startX = margin ; // Posición inicial X del texto desde la izquierda

	        // Divide el texto del usuario en múltiples líneas según el ancho disponible
	        List<String> lineasDocumento = crearPaginaNombreUsuarioObtenerLineas(userName, width, fontSize, PDType1Font.HELVETICA_BOLD) ;

	        // Calcula la posición inicial Y para centrar el texto verticalmente
	        
	        // Altura total del texto calculada
	        float textHeight = lineasDocumento.size() * leading ;
	        float startY = (newPage.getMediaBox().getHeight() + textHeight) / 2 ;

	        // Inicia el flujo de contenido de texto
	        contentStream.beginText() ;
	        contentStream.setFont(PDType1Font.HELVETICA_BOLD, fontSize) ;
	        contentStream.newLineAtOffset(startX, startY) ;

	        // Itera sobre cada línea de texto y la escribe centrada en la página
	        for (String lineaDocumento : lineasDocumento)
	        {
	        	// Escribe la línea en el documento
	            this.crearPaginaNombreUsuarioEscribeLineaDocumento(newPage, contentStream, fontSize, leading, startX, lineaDocumento) ;
	        }

	        // Finaliza el flujo de contenido de texto
	        contentStream.endText() ;
		}
		catch (IOException ioException)
		{
			String errorString = "IOException mientras se creaba la página con el nombre de usuario" ;
			
			log.error(errorString, ioException) ;
			throw new PrinterClientException(errorString, ioException) ;			
		}
		finally
		{
			if (contentStream != null)
			{
				try
				{
					contentStream.close() ;
				}
				catch (IOException ioException)
				{
					String errorString = "IOException mientras se cerraba PD Page Extra en la creación de la página con el nombre de usuario" ;
					
					log.error(errorString, ioException) ;
					throw new PrinterClientException(errorString, ioException) ;
				}
			}
		}
		
		return outcome ;
	}

	/**
	 * 
	 * @param newPage nueva página del documento del usuario
	 * @param contentStream content stream
	 * @param fontSize tamaño de fuente
	 * @param leading espacio entre líneas del texto
	 * @param startX punto de comienzo eje X de la línea
	 * @param lineaDocumento línea del documento
	 * @throws PrinterClientException con un error
	 */
	private void crearPaginaNombreUsuarioEscribeLineaDocumento(PDPage newPage, PDPageContentStream contentStream, float fontSize,
															   float leading, float startX, String lineaDocumento) throws PrinterClientException
	{
		try
		{
			// Calcula el ancho de cada línea de texto
			float textWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(lineaDocumento) / 1000 * fontSize ;
			
			// Calcula el desplazamiento X para centrar la línea
			float offsetX = (newPage.getMediaBox().getWidth() - textWidth) / 2 ;
			
			// Ajusta la posición horizontal
			contentStream.newLineAtOffset(offsetX - startX, 0) ;
			
			// Escribe la línea en la página
			contentStream.showText(lineaDocumento) ;
			
			// Mueve el cursor a la siguiente línea
			contentStream.newLineAtOffset(startX - offsetX, -leading) ;
		}
		catch (IOException ioException)
		{
			String errorString = "IOException mientras se creaba la línea de la página con el nombre de usuario" ;
			
			log.error(errorString, ioException) ;
			throw new PrinterClientException(errorString, ioException) ;			
		}
	}
	
	/** 
	 * Método auxiliar para dividir el texto en líneas ajustadas al ancho disponible.
	 * Este método divide el texto en varias líneas según el ancho máximo permitido,
	 * utilizando la fuente y el tamaño de fuente especificados.
	 * 
	 * @param text El texto completo que se desea dividir en líneas.
	 * @param width El ancho máximo disponible para cada línea de texto.
	 * @param fontSize El tamaño de la fuente que se está utilizando para el texto.
	 * @param font La fuente (`PDFont`) que se usará para calcular el ancho del texto.
	 * @return Una lista de cadenas (`List<String>`) donde cada elemento representa una línea de texto ajustada.
	 * @throws PrinterClientException Si ocurre un error relacionado con la impresión durante el procesamiento del texto.
	 * @throws IOException Si ocurre un error de entrada/salida al calcular el ancho del texto.
	 */
	private List<String> crearPaginaNombreUsuarioObtenerLineas(String text, float width, float fontSize, PDFont font) throws PrinterClientException
	{
	    // Inicializamos una lista para almacenar las líneas de texto resultantes
	    List<String> lines = new ArrayList<>() ;
	    
	    // Utilizamos StringBuilder para construir cada línea de texto de forma eficiente
	    StringBuilder line = new StringBuilder() ;

	    try
	    {
	    	// Iteramos sobre cada palabra en el texto separado por espacios
	        for (String word : text.split(" "))
	        {
	            // Calculamos el ancho de la línea actual si agregamos la nueva palabra
	            String testLine = line.length() == 0 ? word : line.toString() + " " + word ;
	            float testLineWidth = font.getStringWidth(testLine) / 1000 * fontSize ;

	            if (testLineWidth > width)
	            {
	                // Si la línea actual supera el ancho permitido, la agregamos a la lista
	                lines.add(line.toString().trim()) ; // Agregar la línea sin espacios en blanco al principio o al final
	                
	                // Reiniciamos el StringBuilder para la siguiente línea con la palabra actual
	                line = new StringBuilder(word) ;
	            }
	            else
	            {
	                // Si no supera el ancho, añadimos la palabra a la línea actual
	                if (line.length() > 0)
	                {
	                    line.append(" ") ;
	                }
	                
	                line.append(word) ;
	            }
	        }

	        // Si hay texto en el StringBuilder al finalizar, lo añadimos como la última línea
	        if (line.length() > 0) {
	            lines.add(line.toString().trim()); // Agregar la última línea sin espacios en blanco al principio o al final
	        }
	    }
	    catch (IOException ioException)
	    {
	        // Manejamos la excepción si ocurre un error de entrada/salida al calcular el ancho del texto
	        String errorString = "IOException mientras se calculaba el ancho del texto para dividirlo en líneas";
	        
	        log.error(errorString, ioException);
	        throw new PrinterClientException(errorString, ioException) ;
	    }
	    
	    return lines ;
	}
	
	/**
	 * @param dtoPrintAction DTO Print Action
	 * @param pdDocument PD Document
	 * @param printerJob Printer JOB
	 * @throws PrinterException con un error
	 */
	private void configurarEimprimirDocumento(DtoPrintAction dtoPrintAction, PDDocument pdDocument, PrinterJob printerJob) throws PrinterException
	{
		// Configuramos la orientación
		this.configurarOrientacion(dtoPrintAction, pdDocument, printerJob) ;
		
		// Configuramos color, caras y copias
		HashPrintRequestAttributeSet attributeSetDocumentoPrincipal = this.configurarColorCarasYcopias(dtoPrintAction, printerJob) ;
		
		// Imprimimos el documento principal
		printerJob.print(attributeSetDocumentoPrincipal) ;
	}
	
	/**
	 * @param dtoPrintAction DTO Print Action
	 * @param pdDocument PD Document
	 * @param printerJob printer job
	 */
	private void configurarOrientacion(DtoPrintAction dtoPrintAction, PDDocument pdDocument, PrinterJob printerJob)
	{
		PDFPageable pageable = new PDFPageable(pdDocument) ;
		PageFormat format = pageable.getPageFormat(0) ;
		
		if (dtoPrintAction.getVertical())
		{
			format.setOrientation(PageFormat.PORTRAIT) ;
		}
		else
		{
			format.setOrientation(PageFormat.LANDSCAPE) ;
		}

		// La hacemos printable
		printerJob.setPrintable(new PDFPrintable(pdDocument), format) ;
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
			outcome.add(Chromaticity.COLOR);
		} 
		else
		{
			outcome.add(Chromaticity.MONOCHROME);
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
	 * @param dtoPrintAction DTO Print Action
	 * @param pdPageExtra PD Page Extra
	 * @param printerJob Printer JOB
	 * @throws PrinterException con un error
	 */
	private void configurarEimprimirPaginaUsuario(DtoPrintAction dtoPrintAction, PDDocument pdPageExtra, PrinterJob printerJob) throws PrinterException
	{
		// Configuramos la impresión de la página del usuario
		HashPrintRequestAttributeSet attributeSetPaginaUsuario = this.configurarPaginaUsuario(dtoPrintAction, pdPageExtra, printerJob) ;
		
		// Imprimimos la página del usuario
		printerJob.print(attributeSetPaginaUsuario) ;
	}
	
	/**
	 * @param dtoPrintAction DTO Print Action
	 * @param pdPageExtra PD Page Extra
	 * @param printerJob printer job
	 * @return mapa con los atributos de la página del usuario configurados
	 */
	private HashPrintRequestAttributeSet configurarPaginaUsuario(DtoPrintAction dtoPrintAction, PDDocument pdPageExtra, PrinterJob printerJob)
	{
		// Configuramos
		HashPrintRequestAttributeSet outcome = new HashPrintRequestAttributeSet() ;
		outcome.add(new Copies(1)) ;
		
		// La hacemos printable
		printerJob.setPrintable(new PDFPrintable(pdPageExtra)) ;
		
		return outcome ;
	}

	/**
	 *  Metodo encargado de cerrar todos los flujos utilizados en el metodo print
	 * @param pdDocument PD Document
	 * @param pdPageExtra PD Page Extra
	 * @param contenidoFichero contenido del fichero
	 * @throws PrinterClientException con un error
	 */
	private void imprimirCierreFlujos(PDDocument pdDocument,PDDocument pdPageExtra, InputStream contenidoFichero) throws PrinterClientException
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

		if (pdPageExtra != null)
		{
			try
			{
				pdPageExtra.close() ;
			}
			catch (IOException ioException)
			{
				String errorString = "IOException mientras se cerraba PD Page Extra en el cierre de flujos de imprimir" ;
				
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
