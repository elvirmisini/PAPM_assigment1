import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;

public class TestImageFilter {

    public static void main(String[] args) throws Exception {

        BufferedImage image = null;
        String srcFileName = null;
        try {
            srcFileName = args[0];
            File srcFile = new File(srcFileName);
            image = ImageIO.read(srcFile);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Usage: java TestImageFilter <image-file>");
            System.exit(1);
        } catch (IIOException e) {
            System.out.println("Error reading image file " + srcFileName + " !");
            System.exit(1);
        }

        System.out.println("Source image: " + srcFileName);

        int w = image.getWidth();
        int h = image.getHeight();
        System.out.println("Image size is " + w + "x" + h);
        System.out.println();

        int[] src = image.getRGB(0, 0, w, h, null, 0, w);
        int[] dst = new int[src.length];

        System.out.println("Starting sequential image filter.");

        long startTime = System.currentTimeMillis();
        ImageFilter filter0 = new ImageFilter(src, dst, w, h);
        filter0.apply();
        long endTime = System.currentTimeMillis();

        long tSequential = endTime - startTime;
        System.out.println("Sequential image filter took " + tSequential + " milliseconds.");

        BufferedImage dstImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        dstImage.setRGB(0, 0, w, h, dst, 0, w);

        String dstName = "Filtered" + srcFileName;
        File dstFile = new File(dstName);
        ImageIO.write(dstImage, "jpg", dstFile);

        System.out.println("Output image: " + dstName);

        // Parallel image filter test
        int[] srcParallel = new int[src.length];
        int[] dstParallel = new int[src.length];
        System.arraycopy(src, 0, srcParallel, 0, src.length);

        System.out.println("Starting parallel image filter.");

        for (int nthreads : new int[]{1, 2, 4, 8, 16, 32}) {
            long startTimeParallel = System.currentTimeMillis();
            ParallelFJImageFilter parallelFilter = new ParallelFJImageFilter(srcParallel, dstParallel, w, h);
            parallelFilter.apply(nthreads);
            long endTimeParallel = System.currentTimeMillis();

            long tParallel = endTimeParallel - startTimeParallel;
            double speedup = (double) tSequential / tParallel;

            System.out.println("Parallel image filter with " + nthreads + " threads took " + tParallel + " milliseconds.");
            System.out.println("Speedup: " + speedup);

            // Verify pixel-wise equality
            assert Arrays.equals(dst, dstParallel) : "Output images are not equal!";
        }
    }
}
