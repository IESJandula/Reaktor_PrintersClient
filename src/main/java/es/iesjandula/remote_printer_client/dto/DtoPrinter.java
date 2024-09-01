package es.iesjandula.remote_printer_client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


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
