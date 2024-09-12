package es.iesjandula.reaktor_printers_client.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Scanner;

import javax.print.PrintService;

import org.springframework.stereotype.Service;

import es.iesjandula.reaktor_printers_client.dto.DtoPrinter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Francisco Manuel Benítez Chico
 */
@Slf4j
@Service
public class PrinterInfoService
{
	/**
	 * @param printer nueva impresora a consultar
	 * @return instancia de la impresora encontrada
	 * @throws PrinterClientException con un error
	 */
	public DtoPrinter obtenerInfoImpresora(PrintService printer) throws PrinterClientException
	{
		DtoPrinter outcome 		= null ;
		
		Process process 		= null ;
		InputStream inputStream = null ;
		Scanner scanner 		= null ; 
		
		try
		{
			// Lanzamos el proceso para que nos informe diciéndole que nos pase la información con tildes (primer comando)
			process 	= Runtime.getRuntime().exec("cmd.exe /c chcp 65001 && " + 
																Constants.PRINTERS_CLIENT_CONFIG_EXEC + File.separator + 
																		"ConsoleApp1.exe \"" + printer.getName() + "\"") ;
			// Obtenemos el flujo de entrada
			inputStream = process.getInputStream() ;
			
			// Leemos con el scanner
			scanner 	= new Scanner(inputStream, "UTF-8");

			// Ignoramos la primera línea que es el mensaje de cambio de página de códigos
			if (scanner.hasNextLine())
			{
			    scanner.nextLine() ; // Ignoramos la línea de "Página de códigos activa: 65001"
			}
			
			// Creamos una instancia de la impresora
			outcome = new DtoPrinter(printer.getName(),
									 Integer.valueOf(scanner.nextLine()),
									 scanner.nextLine(),
									 Integer.valueOf(scanner.nextLine()),
									 new Date()) ;
		}
		catch (IOException ioException)
		{
			String errorString = "IOException mientras se obtenía información de la impresora " + printer.getName() ;
			
			log.error(errorString, ioException) ;
			throw new PrinterClientException(errorString, ioException) ;
		}
		finally
		{
			if (inputStream != null)
			{
				try
				{
					inputStream.close() ;
				}
				catch (IOException ioException)
				{
					log.error("IOException en inputStream mientras se cerraba el flujo de datos", ioException) ;
				}
			}
			
			if (scanner != null)
			{
				scanner.close() ;
			}
		}
		
		return outcome ;
	}
}
