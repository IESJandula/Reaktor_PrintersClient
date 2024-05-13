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
                ManagementObjectSearcher jobSearcher = new ManagementObjectSearcher("SELECT * FROM Win32_PrintJob WHERE Name='" + printerName + "'");
                int jobCount = jobSearcher.Get().Count;
                Console.WriteLine(jobCount);
            }
        }

        public static string ConvertStatus(string status)
        {
            switch (status)
            {
                case "512":
                    return "Busy: The printer is busy.";
                case "4194304":
                    return "DoorOpen: A door on the printer is open.";
                case "2":
                    return "Error: The printer cannot print due to an error condition.";
                case "32768":
                    return "Initializing: The printer is initializing.";
                case "256":
                    return "IOActive: The printer is exchanging data with the print server.";
                case "32":
                    return "ManualFeed: The printer is waiting for a user to place print media in the manual feed bin.";
                case "0":
                    return "None: Status is not specified.";
                case "4096":
                    return "NotAvailable: Status information is unavailable.";
                case "262144":
                    return "NoToner: The printer is out of toner.";
                case "128":
                    return "Offline: The printer is offline.";
                case "2097152":
                    return "OutOfMemory: The printer has no available memory.";
                case "2048":
                    return "OutputBinFull: The printer's output bin is full.";
                case "524288":
                    return "PagePunt: The printer is unable to print the current page.";
                case "8":
                    return "PaperJam: The paper in the printer is jammed.";
                case "16":
                    return "PaperOut: The printer does not have, or is out of, the type of paper needed for the current print job.";
                case "64":
                    return "PaperProblem: The paper in the printer is causing an unspecified error condition.";
                case "1":
                    return "Paused: The print queue is paused.";
                case "4":
                    return "PendingDeletion: The print queue is deleting a print job.";
                case "16777216":
                    return "PowerSave: The printer is in power save mode.";
                case "1024":
                    return "Printing: The device is printing.";
                case "16384":
                    return "Processing: The device is doing some kind of work, which need not be printing if the device is a combination printer, fax machine, and scanner.";
                case "8388608":
                    return "ServerUnknown: The printer is in an error state.";
                case "131072":
                    return "TonerLow: Only a small amount of toner remains in the printer.";
                case "1048576":
                    return "UserIntervention: The printer requires user action to correct an error condition.";
                case "8192":
                    return "Waiting: The printer is waiting for a print job.";
                case "65536":
                    return "WarmingUp: The printer is warming up.";
                default:
                    return "Unknown status code: " + status;
            }
        }
    }
}
