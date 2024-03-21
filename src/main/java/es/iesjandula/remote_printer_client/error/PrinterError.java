package es.iesjandula.remote_printer_client.error;

public class PrinterError extends Exception
{

	/**
	 * @param message
	 * @param cause
	 */
	public PrinterError(String message, Throwable cause)
	{
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 */
	public PrinterError(String message)
	{
		super(message);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -2704155745550886249L;

}
