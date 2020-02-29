package CLIDownloader;


//import com.sun.xml.internal.ws.api.message.ExceptionHasMessage

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static java.lang.System.out;

public class Main
{
    public static String myURL;

    // Steps to successfully download a file
    private static void printUsage(String[] args) {
        System.out.println("Welcome to CLI Based File Downloader......... Correct usage is shown below");
        System.err.println("\nUsage: java -jar ./IllumionCLIBasedDownloader-1.0-SNAPSHOT.jar  <URL> <number of threads>");
        System.err.println();
    }
    private static double joinTime;
    private static double downloadTime;

    public static void main(String[] args) throws InterruptedException
    {
        if (args.length == 0)
        {
            printUsage(args);
            System.exit(0);
        }

        myURL = args[0]; // Read the url from command line

        //Take number of threads as input from user
        int partsCount = Integer.parseInt(args[1]); // Number of threads used for multi-threading

        int processors = Runtime.getRuntime().availableProcessors();
        if (partsCount > (2 * processors)) {
            out.println("Number of threads cannot exceed " + 2 * processors + " for optimal efficiency");
            System.exit(0);
        }

        // Creating an object which keeps track of the progress of our download
        ProgressBar progress = new ProgressBar();


        // Start new download with the given URL
        Download newDownloaded = new Download(myURL, partsCount, progress);

        // Start the download.
        Instant start = Instant.now();
        newDownloaded.startThread();

        // Verify the url inputted by the user
        System.out.println("Sending HTTP request................");
        synchronized (progress)
        {
            // verify the url user has inputted and wait till its verified or an expection occurs
            while (progress.myURLVerifyResult.responseCode == 0 && progress.ex == null)
            {
                progress.wait();
            }

            if (progress.ex == null)
            {
                // If no exception was thrown, URL verification succeeds.
                System.out.println("Response code: " + progress.myURLVerifyResult.responseCode);

                out.println("Downloading file");
            }
            else
            {
                // Else print the error message and exit.
                printErrorMessage(progress.ex);
            }
        }
        System.out.println();

        // Wait for the download to finish
        Instant downloadFinish = null;

        synchronized (progress)
        {
            // Wait until the download finishes or an exception is thrown.
            while (!progress.downloadFinished && progress.ex == null)
            {
                progress.wait();
            }
            if (progress.ex == null)
            {
                // If no exception was thrown. the file was downloaded successfully.
                downloadFinish = Instant.now();
                downloadTime = ((double) (Duration.between(start,
                        downloadFinish).toMillis())) / 1000;

                System.out.println("\n\nTotal download time: " + downloadTime);
            }
            else
            {
                // Else print the error message and exit.
                printErrorMessage(progress.ex);
            }
        }

        // Wait for the parts to finish joining.
        Instant joinFinishedTime;

        synchronized (progress)
        {
            // Wait until all parts finish joining or an exception is thrown.
            while (!progress.joinPartsFinished && progress.ex == null)
            {
                progress.wait();
            }

            if (progress.ex == null)
            {
                // If no exception is thrown, parts joining succeeds.
                joinFinishedTime = Instant.now();
                joinTime = ((double) (Duration.between(downloadFinish,
                        joinFinishedTime).toMillis())) / 1000;

                System.out.println("Total join time: " + joinTime);

                //Print the total time which is download time plus the join time
                out.println("Total time: " + (downloadTime+joinTime));
            }
            else
            {
                // Else print the error message and exit.
                printErrorMessage(progress.ex);
            }
        }

        // Wait for the main download thread to end.
        try
        {
            newDownloaded.joinThread();
        }
        catch (InterruptedException ex)
        {
            System.out.println("Exiting, error occurred");
        }

        // Print the current time
        Date date = new Date();
        System.out.println("Finished downloading at ");
        System.out.print(new Timestamp(date.getTime()));
    }

    private static void printErrorMessage(Exception ex) {
        /*
         * Print the appropriate error message from the exception caught.
         */
        if (ex instanceof ConnectException) {
            ConnectException connectException = (ConnectException) ex;
            System.err.println("\nFailed to connect to the given URL: " + connectException.getMessage());
            System.err.println("\nCheck your internet connection or URL again.");

        } else if (ex instanceof MalformedURLException) {
            System.err.println("Invalid URL: " + ex.getMessage());

        } else if (ex instanceof IOException) {
            System.err.println("\nFailed to open the output file: "
                    + ex.getMessage());
        } else if (ex instanceof InterruptedException) {
            System.err.println("\nOne of the thread was interrupted: "
                    + ex.getMessage());
        }

        /*
         * Exit the program.
         */
        System.err.println("\nExiting!");
        System.exit(0);
    }
}
