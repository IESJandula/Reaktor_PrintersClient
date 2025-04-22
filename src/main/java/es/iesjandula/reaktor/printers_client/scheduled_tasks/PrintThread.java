package es.iesjandula.reaktor.printers_client.scheduled_tasks;

import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.Finishings;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.Sides;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;

import es.iesjandula.reaktor.printers_client.dto.DtoPrintAction;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Francisco Manuel Benítez Chico
 */
@Slf4j
public class PrintThread extends Thread
{
	/** DTO Print Action */
	private DtoPrintAction dtoPrintAction ;
	
	/** PD Document */
	private PDDocument pdDocument ; 
	
	/** Printer JOB */
	private PrinterJob printerJob ;
	
	/** Excepción sucedida mientras se imprimía */
	private Exception exception ;
	
	public PrintThread(DtoPrintAction dtoPrintAction, PDDocument pdDocument, PrinterJob printerJob)
	{
		this.dtoPrintAction = dtoPrintAction ;
		this.pdDocument		= pdDocument ;
		this.printerJob		= printerJob ;
	}

	@Override
	public void run()
	{
        try
        {
            // Configuramos e imprimimos el documento
            this.configurarEimprimirDocumento();
        }
        catch (Exception exception)
        {
        	// Capturamos la excepción
            this.exception = exception ;
            
            // Logueamos
            log.error("Error durante la impresión: {}", this.exception.getMessage(), this.exception) ;
        }
	}
	
	/**
	 * @throws PrinterException con un error
	 */
	private void configurarEimprimirDocumento() throws PrinterException
	{
	    // Configuramos color, caras y copias
	    HashPrintRequestAttributeSet attributeSetDocumentoPrincipal = this.configurarColorCarasYcopias() ;

	    // Configuramos la orientación (actualizado)
	    this.configurarOrientacion(attributeSetDocumentoPrincipal) ;
	    
	    // Logueamos
	    log.info("PRINT - IMPRESION - Se va a enviar a la cola de impresión") ;

	    // Si hay páginas seleccionadas, configuramos solo esas páginas para imprimir
	    if (this.dtoPrintAction.getSelectedPages() != null && !this.dtoPrintAction.getSelectedPages().isEmpty())
		{
	        this.imprimirPaginasSeleccionadas(attributeSetDocumentoPrincipal);
	    }
		else
		{
	        // Imprimimos el documento completo
	        this.printerJob.print(attributeSetDocumentoPrincipal);
	    }
	    
	    // Logueamos
	    log.info("PRINT - IMPRESION - Se ha realizado la impresión") ;
	}
	
	/**
	 * Imprime sólo las páginas seleccionadas
	 * 
	 * @param attributeSetDocumentoPrincipal conjunto de atributos de impresión
	 * @throws PrinterException si hay un error durante la impresión
	 */
	private void imprimirPaginasSeleccionadas(HashPrintRequestAttributeSet attributeSetDocumentoPrincipal) throws PrinterException
	{
	    try
		{
	        // Obtener la cadena de páginas seleccionadas (formato: "1,3,5-7,9")
	        String selectedPagesStr = this.dtoPrintAction.getSelectedPages();
	        log.info("Imprimiendo páginas seleccionadas: {}", selectedPagesStr);
	        
	        // Preprocesar la cadena para eliminar cualquier formato JSON
	        selectedPagesStr = preprocessSelectedPagesString(selectedPagesStr);
	        
	        // Crear un array para marcar qué páginas imprimir
	        boolean[] pagesToPrint = new boolean[this.pdDocument.getNumberOfPages()];
	        
	        // Procesar la cadena de páginas seleccionadas
	        String[] parts = selectedPagesStr.split(",");
	        for (String part : parts)
			{
	            part = part.trim();
	            if (!part.isEmpty())
				{
	            
					if (part.contains("-"))
					{
						// Rango de páginas
						String[] range = part.split("-");
						int start = Integer.parseInt(range[0].trim()) - 1; // Convertir a índice base 0
						int end = Integer.parseInt(range[1].trim()) - 1;
						
						// Validar rango
						start = Math.max(0, start);
						end = Math.min(pdDocument.getNumberOfPages() - 1, end);
						
						// Marcar todas las páginas en el rango
						for (int i = start; i <= end; i++)
						{
							pagesToPrint[i] = true;
						}
					}
					else
					{
						// Página individual
						try
						{
							int pageIndex = Integer.parseInt(part) - 1; // Convertir a índice base 0
							
							// Validar índice de página
							if (pageIndex >= 0 && pageIndex < pdDocument.getNumberOfPages())
							{
								pagesToPrint[pageIndex] = true;
							}
						}
						catch (NumberFormatException e)
						{
							log.warn("No se pudo parsear el número de página: {}", part);
						}
					}
				}
	        }
	        
	        // Crear un nuevo documento con solo las páginas seleccionadas
	        PDDocument selectedPagesDoc = new PDDocument();
	        for (int i = 0; i < pagesToPrint.length; i++)
			{
	            if (pagesToPrint[i])
				{
	                selectedPagesDoc.importPage(pdDocument.getPage(i));
	            }
	        }
	        
	        // Configurar la impresión con el nuevo documento
	        PrinterJob selectedPrinterJob = PrinterJob.getPrinterJob();
	        selectedPrinterJob.setPrintService(this.printerJob.getPrintService());
	        
	        // Utilizar la misma configuración de orientación
	        if (this.dtoPrintAction.getVertical())
			{
	            PDFPageable pageable = new PDFPageable(selectedPagesDoc);
	            PageFormat format = pageable.getPageFormat(0);
	            format.setOrientation(PageFormat.PORTRAIT);
	            selectedPrinterJob.setPrintable(new PDFPrintable(selectedPagesDoc), format);
	        }
			else
			{
	            selectedPrinterJob.setPrintable(new PDFPrintable(selectedPagesDoc, Scaling.SHRINK_TO_FIT));
	        }
	        
	        // Imprimir el documento con las páginas seleccionadas
	        selectedPrinterJob.print(attributeSetDocumentoPrincipal);
	        
	        // Cerrar el documento temporal
	        selectedPagesDoc.close();
	        
	    }
		catch (Exception e)
		{
	        log.error("Error al imprimir páginas seleccionadas", e);
	        throw new PrinterException("Error al imprimir páginas seleccionadas: " + e.getMessage());
	    }
	}
	
	/**
	 * Preprocesa la cadena de páginas seleccionadas para eliminar cualquier formato JSON
	 * 
	 * @param input la cadena de entrada que podría estar en formato JSON
	 * @return una cadena limpia con solo los números y rangos separados por comas
	 */
	private String preprocessSelectedPagesString(String input)
	{
	    if (input == null || input.isEmpty())
		{
	        return "";
	    }
	    
	    // Eliminar corchetes de array JSON si están presentes
	    String processed = input.replaceAll("\\[|\\]|\"", "");
	    
	    log.info("Cadena de páginas procesada: {}", processed);
	    return processed;
	}
	
	/**
	 * @return mapa con los atributos de la página a imprimir configurados
	 */
	private HashPrintRequestAttributeSet configurarColorCarasYcopias()
	{
		HashPrintRequestAttributeSet outcome = new HashPrintRequestAttributeSet();
		
		// Configuramos color
		if (this.dtoPrintAction.getBlackAndWhite())
		{
			outcome.add(Chromaticity.MONOCHROME);
		} 
		else
		{
			outcome.add(Chromaticity.COLOR);			
		}
		
		// Configuramos caras
		if (this.dtoPrintAction.getTwoSides())
		{
			outcome.add(Sides.DUPLEX);
		} 
		else
		{
			outcome.add(Sides.ONE_SIDED);
		}

		// Configuramos grapado
		if (this.dtoPrintAction.getStapling())
		{
			if (this.dtoPrintAction.getVertical())
			{
				outcome.add(Finishings.STAPLE_TOP_LEFT);
			}
			else
			{
				outcome.add(Finishings.STAPLE_BOTTOM_LEFT);
			}
		}
		else
		{
			outcome.add(Finishings.NONE);
		}

		
		// Configuramos copias
		outcome.add(new Copies(this.dtoPrintAction.getCopies())) ;
		
		return outcome ;
	}
	
	/**
	 * @param attributeSetDocumentoPrincipal attribute set documento principal
	 */
	private void configurarOrientacion(HashPrintRequestAttributeSet attributeSetDocumentoPrincipal)
	{
	    // Ajustamos la orientación según el documento
	    if (this.dtoPrintAction.getVertical())
	    {
			PDFPageable pageable = new PDFPageable(this.pdDocument) ;
			PageFormat format = pageable.getPageFormat(0) ;
			
			format.setOrientation(PageFormat.PORTRAIT) ;
	    	
			// La hacemos printable
			this.printerJob.setPrintable(new PDFPrintable(this.pdDocument), format) ;
	    }
	    else
	    {
	    	// Horizontal
	        attributeSetDocumentoPrincipal.add(OrientationRequested.LANDSCAPE) ;
	        
		    // Establecemos el printable al printerJob adaptándola al espacio
	        this.printerJob.setPrintable(new PDFPrintable(this.pdDocument, Scaling.SHRINK_TO_FIT)) ;
	    }
	}

	/**
	 * @return the exception
	 */
	public Exception getException()
	{
		return this.exception ;
	}
}
