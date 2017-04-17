package test;

import java.awt.image.BufferedImage;
import java.io.File;

public class prog implements Runnable
{
    // Instance vars generated from List<ParamDec>
    public int gint0;
    public boolean gbul0;
    public int gint1;
    public boolean gbul1;
    public File f;
    public java.net.URL u;
    // Constructor
    public prog(String[] args) throws java.net.MalformedURLException
    {
        // TODO: Initialize with values from args
//        this.gint0 = Integer.parseInt(args[0]);
//        this.gbul0 = Boolean.parseBoolean(args[1]);
//        this.gint1 = Integer.parseInt(args[2]);
//        this.gbul1 = Boolean.parseBoolean(args[3]);
        args[0] = "D:\\theyenaman\\Pictures\\data_irl.png";
        this.f = new File(args[0]);
//        this.u = new java.net.URL(args[5]);
    }

    public static void main(String[] args) throws java.net.MalformedURLException
    {
        (new prog(args)).run();
    }

    @Override
    public void run()
    {
        // declaration list
        int lint0;
        int lint1;
        
        BufferedImage im = PLPRuntimeImageIO.readFromFile(this.f);
        PLPRuntimeFilterOps.blurOp(im, null);
    }
}
