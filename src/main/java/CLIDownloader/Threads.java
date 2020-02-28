package CLIDownloader;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.time.Duration;
import java.time.Instant;

public class Threads implements Runnable
{
    private Thread myThread;
    private long myStartingByte;
    private long myEndingByte;
    private long myPartSize;
    private int myParts;
    private URL myURL;
    private long totalDownloadSize;
    private long partsAlreadyDownloaded;

    private final String myFileName;

    private final ProgressBar myProgress;

    //constructing a download object with url, start and end byte range for the downloads
    public Threads(URL url, long startByte, long endByte, long partSize, int part, ProgressBar progress)
    {
        if (startByte >= endByte)
        {
            throw new RuntimeException("The start byte cannot be larger than " + "the end byte!");
        }

        myStartingByte = startByte;
        myEndingByte = endByte;
        myPartSize = partSize;
        //mResume = resume;
        myURL = url;
        myParts = part;
        totalDownloadSize = 0;
        partsAlreadyDownloaded = 0;

        // Getting file name.
        myFileName = "." + (new File(myURL.toExternalForm()).getName() + ".part" + myParts);

        // Initializing  the thread
        myThread = new Thread(this, "Part #" + part);

        myProgress = progress;
    }

    public void startDownload()
    {
        myThread.start();
    }

    public void joinThread() throws InterruptedException
    {
        myThread.join();
    }

    public HttpURLConnection getHttpConnection() throws IOException
    {
        // Connect to the URL
        HttpURLConnection conn = (HttpURLConnection) myURL.openConnection();

        String downloadRange = "bytes=" + myStartingByte + "-" + myEndingByte;
        conn.setRequestProperty("Range", downloadRange);
        conn.connect();

        // Return the connection.
        return conn;
    }

    public void downloadToFile(HttpURLConnection conn) throws IOException
    {
        // Get the input stream.
        InputStream is = conn.getInputStream();

        // Size of the chunk of data to be downloaded and written to the
        // output file at a time.
        int chunkSize = (int) Math.pow(2, 14);

        try (DataInputStream dataIn = new DataInputStream(is))
        {
            // Get the file's length.
            long contentLength = conn.getContentLengthLong();
            contentLength = contentLength + partsAlreadyDownloaded;

            // Read a chunk of given size at time to write to the output file.
            byte[] dataArray = new byte[chunkSize];
            int result;

            //whether to overwrite the file or not
            boolean overwrite = true;

            synchronized (myProgress)
            {
                myProgress.downloadedCount += totalDownloadSize;
                myProgress.notifyAll();
            }

            // While the total downloaded size is still smaller than the
            // content length from the connection, keep reading data.
            while (totalDownloadSize < contentLength)
            {
                Instant start = myProgress.startDownloadTimeStamp;
                result = dataIn.read(dataArray, 0, chunkSize);
                Instant stop = Instant.now();
                long time = Duration.between(stop, start).getNano();

                if (result == -1)
                {
                    break;
                }

                totalDownloadSize = totalDownloadSize + result;
                writeToFile(dataArray, result, overwrite);
                overwrite = false;

                synchronized (myProgress)
                {
                    myProgress.downloadedCount = result + myProgress.downloadedCount;
                    myProgress.time = time + myProgress.time;
                    myProgress.sizeChange = result + myProgress.sizeChange;
                    myProgress.percentageCount++;

                    myProgress.updateProgressBar();
                    if (myProgress.percentageCount == 1) {
                        myProgress.time = 0;
                        myProgress.sizeChange = 0;
                        myProgress.percentageCount = 0;
                    }

                    myProgress.notifyAll();
                }
            }
        }
    }


    public void writeToFile(byte[] bytes, int bytesToWrite, boolean overwrite) throws IOException
    {
        try (FileOutputStream fileout = new FileOutputStream(myFileName, !overwrite))
        {

            //Using file channel to write to the output file
            FileChannel outChannel = fileout.getChannel();

            // use byte buffer to wrap the array
            ByteBuffer data = ByteBuffer.wrap(bytes, 0, bytesToWrite);

            // Writing the data to the output file.
            outChannel.write(data);
        }
    }


    public long getDownloadedSize()
    {
        //returns the downloaded size
        return totalDownloadSize;
    }


    public long getPartSize()
    {
        //returns the size of the current part
        return myPartSize;
    }

    @Override
    public void run()
    {
        try
        {
            // Connect to the URL
            HttpURLConnection conn = getHttpConnection();

            // Download to file
            downloadToFile(conn);
        }
        catch (IOException ex)
        {
            synchronized (myProgress)
            {
                myProgress.ex = ex;
                myProgress.notifyAll();
            }
        }
    }
}
