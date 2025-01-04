package es.iesjandula.reaktor.printers_client.utils;

import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * @author Francisco Manuel Benítez Chico
 */
public class PrinterClientException extends Exception
{
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -2704155745550886249L ;
	
	/** Excepcion de error para la excepcion */
	private Exception exception ;
	
	/**
	 * @param message mensaje del error
	 */
	public PrinterClientException(String message) 
	{
		super(message);
	}

	/**
	 * 
	 * @param message mensaje del error
	 * @param exception excepcion del error
	 */
	public PrinterClientException(String message, Exception exception) 
	{
		super(message,exception) ;
		
		this.exception = exception ;
	}
	
	/**
	 * @return traza de error en formato string 
	 */
	public String getException()
	{
		String outcome = null ;
		
		if (this.exception != null)
		{
			outcome = ExceptionUtils.getStackTrace(this.exception) ;
		}
		
		return outcome ;
	}
}
