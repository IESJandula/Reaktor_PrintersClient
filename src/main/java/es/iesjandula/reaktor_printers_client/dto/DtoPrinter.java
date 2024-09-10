package es.iesjandula.reaktor_printers_client.dto;

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
	
	private String name;
	
	private int statusId;
	
	private String status;
	
	private int printingQueue;
	
}
