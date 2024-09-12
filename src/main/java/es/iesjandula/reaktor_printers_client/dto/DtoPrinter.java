package es.iesjandula.reaktor_printers_client.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Francisco Manuel Benítez Chico
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class DtoPrinter
{
	/** Atributo - Name */
	private String name ;
	
	/** Atributo - Status ID */
	private int statusId ;
	
	/** Atributo - Status */
	private String status ;
	
	/** Atributo - Printing queue */
	private int printingQueue ;
	
	/** Atributo - Last update */
	private Date lastUpdate ;
}
