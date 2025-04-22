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

	    // Imprimimos el documento principal
	    this.printerJob.print(attributeSetDocumentoPrincipal) ;
	    
	    // Logueamos
	    log.info("PRINT - IMPRESION - Se ha realizado la impresión") ;
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
