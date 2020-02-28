package CLIDownloader;

import java.time.Instant;

public class ProgressBar
{
    public VerifyURL myURLVerifyResult;
    public Exception ex;
    public boolean downloadFinished;
    public boolean joinPartsFinished;

    public long downloadedCount;
    public long time;
    public long sizeChange;
    public long percentageCount;

    public Instant startDownloadTimeStamp;

    public ProgressBar() {
        myURLVerifyResult =  new VerifyURL(0, -1);
        ex = null;
        downloadFinished = false;
        joinPartsFinished = false;
        downloadedCount = 0;
        time = 0;
        sizeChange = 0;
    }

    public void updateProgressBar()
    {
        /** create a progress bar which tracks the progress of the download and keep on
         * updating it as the download proceeds
         */


        // Get the percentage of the part downloaded
        long contentSize = myURLVerifyResult.contentLength;

        double percent = ((double) downloadedCount / (double) contentSize) * 100;
        percent = (double) ((int)Math.round(percent * 100)) / 100;


        String complete = new String(new char[(int) percent]).replace("\0", "*");
        String incomplete = new String(new char[100 - (int) percent]).replace("\0", " ");

        System.out.print("(" + complete + incomplete + ")" + String.valueOf(percent) + "% " + "\r");


    }
}
