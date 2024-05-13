package es.iesjandula.remote_printer_client.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Data
public class Printer
{
	
	private String name;
	
	private int statusId;
	
	private String status;
	
	private int printingQueue;
	
}
