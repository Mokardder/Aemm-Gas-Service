package android.iocl.dac_collector.Interface;

/**
 * Call this when the service is complete
 */
public interface OnCompleteInterface
{
    public void onComplete(int count, String who);
}