package es.iesjandula.reaktor.printers_client.dto;

import java.io.InputStream;

/**
 * @author Francisco Manuel Benítez Chico
 */
public class DtoPrintAction
{
	/** Atributo - ID */
    private String id ;
    
    /** Atributo - User */
    private String user ;
    
    /** Atributo - Printer */
    private String printer ;
    
    /** Atributo - Status */
    private String status ;
    
    /** Atributo - File name */
    private String fileName ;

    /** Atributo - Copies */
    private int copies ;
    
    /** Atributo - Black and white */
    private Boolean blackAndWhite ;

    /** Atributo - Vertical */
    private Boolean vertical ;

    /** Atributo - Sides */
    private Boolean twoSides ;
    
    /** Atributo - Páginas seleccionadas */
    private String selectedPages ;

    /** Atributo - Contenido del fichero */
    private InputStream contenidoFichero ;

    /**
     * Default constructor
     */
    public DtoPrintAction()
    {
    	// Empty
    }
    
    /**
     * Obtiene el identificador único de la acción de impresión.
     * 
     * @return el identificador único (id)
     */
    public String getId()
    {
        return this.id ;
    }

    /**
     * Establece el identificador único de la acción de impresión.
     * 
     * @param id el identificador único (id) a establecer
     */
    public void setId(String id)
    {
        this.id = id ;
    }

    /**
     * Obtiene el nombre del usuario que realiza la acción de impresión.
     * 
     * @return el nombre del usuario
     */
    public String getUser()
    {
        return this.user ;
    }

    /**
     * Establece el nombre del usuario que realiza la acción de impresión.
     * 
     * @param user el nombre del usuario a establecer
     */
    public void setUser(String user)
    {
        this.user = user ;
    }

    /**
     * Obtiene el nombre de la impresora.
     * 
     * @return el nombre de la impresora
     */
    public String getPrinter()
    {
        return this.printer ;
    }

    /**
     * Establece el nombre de la impresora.
     * 
     * @param printer el nombre de la impresora a establecer
     */
    public void setPrinter(String printer)
    {
        this.printer = printer ;
    }

    /**
     * Obtiene el estado de la acción de impresión.
     * 
     * @return el estado de la impresión
     */
    public String getStatus()
    {
        return this.status ;
    }

    /**
     * Establece el estado de la acción de impresión.
     * 
     * @param status el estado a establecer
     */
    public void setStatus(String status)
    {
        this.status = status ;
    }

    /**
     * Obtiene el nombre del archivo que se imprimirá.
     * 
     * @return el nombre del archivo
     */
    public String getFileName()
    {
        return this.fileName ;
    }

    /**
     * Establece el nombre del archivo que se imprimirá.
     * 
     * @param fileName el nombre del archivo a establecer
     */
    public void setFileName(String fileName)
    {
        this.fileName = fileName ;
    }

    /**
     * Obtiene el número de copias que se deben imprimir.
     * 
     * @return el número de copias
     */
    public int getCopies()
    {
        return this.copies ;
    }

    /**
     * Establece el número de copias que se deben imprimir.
     * 
     * @param copies el número de copias a establecer
     */
    public void setCopies(int copies)
    {
        this.copies = copies ;
    }

	/**
	 * @return the blackAndWhite
	 */
	public Boolean getBlackAndWhite()
	{
		return this.blackAndWhite ;
	}

	/**
	 * @param blackAndWhite the blackAndWhite to set
	 */
	public void setBlackAndWhite(Boolean blackAndWhite)
	{
		this.blackAndWhite = blackAndWhite ;
	}

	/**
	 * @return the vertical
	 */
	public Boolean getVertical()
	{
		return this.vertical ;
	}

	/**
	 * @param vertical the vertical to set
	 */
	public void setVertical(Boolean vertical)
	{
		this.vertical = vertical ;
	}

	/**
	 * @return the twoSides
	 */
	public Boolean getTwoSides()
	{
		return this.twoSides ;
	}

	/**
	 * @param twoSides the twoSides to set
	 */
	public void setTwoSides(Boolean twoSides)
	{
		this.twoSides = twoSides ;
	}

	/**
	 * @return the selectedPages
	 */
	public String getSelectedPages()
	{
		return this.selectedPages ;
	}

	/**
	 * @param selectedPages the selectedPages to set
	 */
	public void setSelectedPages(String selectedPages)
	{
		this.selectedPages = selectedPages ;
	}

	/**
	 * @return the contenidoFichero
	 */
	public InputStream getContenidoFichero()
	{
		return this.contenidoFichero ;
	}

	/**
	 * @param contenidoFichero the contenidoFichero to set
	 */
	public void setContenidoFichero(InputStream contenidoFichero)
	{
		this.contenidoFichero = contenidoFichero ;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder() ;
		
		builder.append("DtoPrintAction [id=") ;
		builder.append(this.id) ;
		
		builder.append(", user=") ;
		builder.append(this.user) ;
		
		builder.append(", printer=") ;
		builder.append(this.printer) ;
		
		builder.append(", status=") ;
		builder.append(this.status) ;
		
		builder.append(", fileName=") ;
		builder.append(this.fileName) ;
		
		builder.append(", copies=") ;
		builder.append(this.copies) ;
		
		builder.append(", blackAndWhite=") ;
		builder.append(this.blackAndWhite) ;
		
		builder.append(", vertical=") ;
		builder.append(this.vertical) ;
		
		builder.append(", twoSides=") ;
		builder.append(this.twoSides) ;
		
		builder.append(", selectedPages=") ;
		builder.append(this.selectedPages) ;
		
		builder.append("]") ;
		
		return builder.toString() ;
	}
}

