using System;
using System.Management;

namespace ConsoleApp1
{
    internal class Program
    {
        static void Main(string[] args)
        {
            
            if (args.Length == 0)
            {
                Console.WriteLine("Por favor proporciona el nombre de la impresora como argumento al ejecutar el programa.");
                return;
            }

            string printerName = args[0]; // El primer argumento es el nombre de la impresora
            ManagementObjectSearcher searcher = new ManagementObjectSearcher("SELECT * FROM Win32_Printer WHERE Name='" + printerName + "'");

            foreach (ManagementObject printer in searcher.Get())
            {
                Console.WriteLine(printer["PrinterState"]); // Estado de la conexión de la impresora
                Console.WriteLine(ConvertStatus(printer["PrinterState"].ToString())); // Estado de la conexión de la impresora
                ManagementObjectSearcher jobSearcher = new ManagementObjectSearcher("SELECT * FROM Win32_PrintJob WHERE Name LIKE '%" + printerName + "%'");
                int jobCount = jobSearcher.Get().Count;
                Console.WriteLine(jobCount);
                string printerPortName = printer["PortName"].ToString();
                Console.WriteLine(printerPortName);
                if (!string.IsNullOrEmpty(printerPortName))
                {
                    ManagementObjectSearcher portSearcher = new ManagementObjectSearcher("SELECT * FROM Win32_TCPIPPrinterPort WHERE Name='" + printerPortName + "'");
                    foreach (ManagementObject port in portSearcher.Get())
                    {
                        Console.WriteLine("Dirección IP de la impresora: " + port["HostAddress"]);
                    }
                }
                else
                {
                    Console.WriteLine("No se pudo obtener el nombre del puerto de la impresora.");
                }
            }

            

        }

        public static string ConvertStatus(string status)
        {
            switch (status)
            {
                case "512":
                    return "Ocupado: La impresora está ocupada.";
                case "4194304":
                    return "Puerta Abierta: Una puerta de la impresora está abierta.";
                case "2":
                    return "Error: La impresora no puede imprimir debido a una condición de error.";
                case "32768":
                    return "Inicializando: La impresora se está inicializando.";
                case "256":
                    return "Intercambio Datos: La impresora está intercambiando datos con el servidor de impresión.";
                case "32":
                    return "Alimentación Manual: La impresora está esperando que el usuario coloque el medio de impresión en la bandeja de alimentación manual.";
                case "0":
                    return "Ninguno: No se especifica el estado.";
                case "4096":
                    return "No Disponible: La información de estado no está disponible.";
                case "262144":
                    return "Sin Toner: La impresora no tiene toner.";
                case "128":
                    return "Fuera De Línea: La impresora está fuera de línea.";
                case "2097152":
                    return "Sin Memoria: La impresora no tiene memoria disponible.";
                case "2048":
                    return "Bandeja Salida Llena: La bandeja de salida de la impresora está llena.";
                case "524288":
                    return "Página No Impresa: La impresora no puede imprimir la página actual.";
                case "8":
                    return "Atasco Papel: El papel en la impresora está atascado.";
                case "16":
                    return "Sin Papel: La impresora no tiene, o se ha quedado sin, el tipo de papel necesario para el trabajo de impresión actual.";
                case "64":
                    return "Problema Papel: El papel en la impresora está causando una condición de error no especificada.";
                case "1":
                    return "Pausado: La cola de impresión está pausada.";
                case "4":
                    return "Eliminación Pendiente: La cola de impresión está eliminando un trabajo de impresión.";
                case "16777216":
                    return "Ahorro De Energía: La impresora está en modo de ahorro de energía.";
                case "1024":
                    return "Imprimiendo: El dispositivo está imprimiendo.";
                case "16384":
                    return "Procesando: El dispositivo está realizando algún tipo de trabajo, que no tiene por qué ser impresión si el dispositivo es una combinación de impresora, fax y escáner.";
                case "8388608":
                    return "Error Servidor Desconocido: La impresora está en un estado de error.";
                case "131072":
                    return "Toner Bajo: Solo queda una pequeña cantidad de toner en la impresora.";
                case "1048576":
                    return "Intervención Usuario: La impresora requiere una acción del usuario para corregir una condición de error.";
                case "8192":
                    return "Esperando: La impresora está esperando un trabajo de impresión.";
                case "65536":
                    return "Calentando: La impresora se está calentando.";
                default:
                    return "Código de estado desconocido: " + status;
            }
        }
    }
}
