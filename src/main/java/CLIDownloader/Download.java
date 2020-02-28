package CLIDownloader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;

public class Download implements Runnable
{

    private final String myURL;
    private final int myParts;
    private final ProgressBar myProgress;
    private final Thread myThread;

    public Download(String urlString, int partsCount, ProgressBar progress)
    {
        myURL = urlString;
        myParts = partsCount;
        myProgress = progress;

        myThread = new Thread(this, "Main thread");
    }

    private VerifyURL checkURLValidity(URL url) throws ConnectException
    {
        // Check the validity of the url and create new connection from the given url
        try
        {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Connect to the created connection.
            conn.setRequestMethod("GET");
            conn.connect();

            // Check for the response code
            int responseCode = conn.getResponseCode();
            long contentSize = conn.getContentLengthLong();

            // Return the content size
            VerifyURL result = new VerifyURL(responseCode, contentSize);

            return result;
        }
        catch (IOException ex)
        {
            throw new ConnectException(ex.getMessage());
        }
    }

    public ArrayList<Threads> downloadingThreads(URL url, long contentSize, int partCount, ProgressBar progress)
    {
        long partSize = contentSize / partCount;

        ArrayList<Threads> downloadThreadsList = new ArrayList<>(partCount);

        for (int i = 0; i < partCount; i++)
        {
            // Calculate the begin and end byte for each part.
            long beginByte = i * partSize;
            long endByte;
            if (i == partCount - 1)
            {
                endByte = contentSize - 1;
            }
            else
            {
                endByte = (i + 1) * partSize - 1;
            }

            long currentPartSize = endByte - beginByte + 1;

            // Create new download threads and start them.
            Threads downloadThread = new Threads(url, beginByte, endByte, currentPartSize, i + 1, progress);
            downloadThreadsList.add(downloadThread);
            downloadThreadsList.get(i).startDownload();
        }

        return downloadThreadsList;
    }

    //merge all the downloaded parts together into one file
    public void joinDownloadedParts(String fileName, ArrayList<Threads> listOfDownloadParts) throws IOException
    {
        String outputFile =  fileName;

        try (RandomAccessFile mainFile = new RandomAccessFile(outputFile, "rw"))
        {
            FileChannel mainChannel = mainFile.getChannel();
            long startPosition = 0;

            for (int i = 0; i < listOfDownloadParts.size(); i++)
            {
                String partName = "." + fileName + ".part" + (i + 1);

                try (RandomAccessFile partFile = new RandomAccessFile(partName, "rw"))
                {
                    long partSize = listOfDownloadParts.get(i).getDownloadedSize();
                    FileChannel partFileChannel = partFile.getChannel();
                    long transferedBytes = mainChannel.transferFrom(partFileChannel,
                            startPosition, partSize);

                    startPosition += transferedBytes;

                    if (transferedBytes != partSize)
                    {
                        throw new RuntimeException("Error joining file! At part: "
                                + (i + 1));
                    }
                }
            }
        }
    }

    //starting the thread
    public void startThread() {
        myThread.start();
    }

    //joining the thread
    public void joinThread() throws InterruptedException {
        myThread.join();
    }

    @Override
    public void run()
    {
        try
        {
            // Get the file name and create the URL object
            String fileName = new File(myURL).getName();
            URL url = new URL(myURL);

            // Check the validity of the URL
            VerifyURL result = checkURLValidity(url);
            long contentSize = result.contentLength;
            int responseCode = result.responseCode;

            if (contentSize == -1 || responseCode != 200)
            {
                String errMessage = "Error while checking URL validity!";
                errMessage += "\nResponse code: " + responseCode;
                errMessage += "\nContent size: " + contentSize;
                throw new RuntimeException(errMessage);
            }

            // Notify the progress object of the result of the check
            synchronized (myProgress)
            {
                myProgress.myURLVerifyResult.contentLength = contentSize;
                myProgress.myURLVerifyResult.responseCode = responseCode;
                myProgress.notifyAll();
            }

            // Start threads to download.
            ArrayList<Threads> listOfDownloadParts;

            myProgress.startDownloadTimeStamp = Instant.now();

            try {
                listOfDownloadParts = downloadingThreads(url, contentSize,
                        myParts, myProgress);
            } catch (RuntimeException ex) {
                throw ex;
            }

            // Wait for the threads to finish downloading
            for (int i = 0; i < listOfDownloadParts.size(); i++)
            {
                Threads currentThread = listOfDownloadParts.get(i);
                currentThread.joinThread();
            }

            // Notify that all parts have finished downloading
            synchronized (myProgress)
            {
                myProgress.downloadFinished = true;
                myProgress.notifyAll();
            }


            // Join the myParts together
            joinDownloadedParts(fileName, listOfDownloadParts);

            // Delete part files
            try
            {
                for (int i = 0; i < listOfDownloadParts.size(); i++)
                {
                    String partName = "." + fileName + ".part" + (i + 1);
                    Path filePath = Paths.get(partName);
                    Files.deleteIfExists(filePath);
                }
            }
            catch (IOException ex)
            {
                // If failed to delete then just ignore the exception.
                // What can we do?
            }

            // Notify that all parts have finished joining.
            synchronized (myProgress)
            {
                myProgress.joinPartsFinished = true;
                myProgress.notifyAll();
            }

        }
        catch (RuntimeException | InterruptedException | IOException ex)
        {
            // If an exception is thrown, put it in the progress object and
            // notify the other threads.
            synchronized (myProgress)
            {
                myProgress.ex = ex;
                myProgress.notifyAll();
            }
        }
    }

}
