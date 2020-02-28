package CLIDownloader;

public class VerifyURL
{
    public int responseCode;
    public long contentLength;


    public VerifyURL(int r, long c) {
        responseCode = r;
        contentLength = c;
    }
}
