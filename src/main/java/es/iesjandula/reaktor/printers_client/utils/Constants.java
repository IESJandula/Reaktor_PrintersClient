package es.iesjandula.reaktor.printers_client.utils;

/**
 * @author Francisco Manuel Benítez Chico
 */
public class Constants
{
	/*********************************************************/
	/*********************** Headers *************************/
	/*********************************************************/

	/** Constante - Header - ID */
	public static final String HEADER_PRINT_ID = "id" ;

	/** Constante - Header - Usuario */
	public static final String HEADER_PRINT_USER = "user" ;

	/** Constante - Header - Content-Disposition */
	public static final String HEADER_PRINT_CONTENT_DISPOSITION = "Content-Disposition" ;

	/** Constante - Header - Impresora */
	public static final String HEADER_PRINT_PRINTER = "printer" ;

	/** Constante - Header - Número de Copias */
	public static final String HEADER_PRINT_COPIES = "copies" ;

	/** Constante - Header - Color */
	public static final String HEADER_PRINT_COLOR = "color" ;

	/** Constante - Header - Orientación */
	public static final String HEADER_PRINT_ORIENTATION = "orientation" ;

	/** Constante - Header - Caras */
	public static final String HEADER_PRINT_SIDES = "sides" ;

	/** Constante - Header - Grapas */
	public static final String HEADER_PRINT_STAPLING = "stapling" ;
	
	/*********************************************************/
	/******************* Response Server *********************/
	/*********************************************************/
	
	/** Response server - Key - Status */
	public static final String RESPONSE_SERVER_KEY_STATUS 	 = "status" ;

	/** Response server - Key - Message*/
	public static final String RESPONSE_SERVER_KEY_MESSAGE 	 = "message" ;

	/** Response server - Key - Exception */
	public static final String RESPONSE_SERVER_KEY_EXCEPTION = "exception" ;
	
	/*********************************************************/
	/*********************** Estados *************************/
	/*********************************************************/
	
	/** Constante - Estado - DONE */
	public static final String STATE_DONE  = "Realizado" ;
	
	/** Constante - Estado - ERROR */
	public static final String STATE_ERROR = "Error" ;
	
	
	/*********************************************************/
	/****************** Ficheros y carpetas ******************/
	/*********************************************************/
	
	/** Nombre de la carpeta de configuracion */
	public static final String PRINTERS_CLIENT_CONFIG 	   = "printers_client_config" ;
	
	/** Nombre de la carpeta de configuracion al ejecutarse */
	public static final String PRINTERS_CLIENT_CONFIG_EXEC = "printers_client_config_exec" ;
}

