
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ForkJoinPool;

public class TestImageFilter {

    private static final int NRSTEPS = 100;
    private static final int[] numberOfThreadsToUse = { 1, 2, 4, 8, 16,32 };
    private static final float[] speedUpValues = { 0.7f, 1.4f, 2.8f, 5.6f, 0f,0.7f };
    private static String sourceFilePath = null;
    private static String sourceFileName = null;
    private static String srcFileName = null;
    private static long timeOfSequentialExecution;
    private static long timeOfParallelExecution;

    public static void main(String[] args) throws Exception {
        BufferedImage image = null;
        try {
            srcFileName = args[0];
            File srcFile = new File(srcFileName);
            sourceFilePath = srcFile.getAbsolutePath();
            sourceFileName = srcFile.getName();
            image = ImageIO.read(srcFile);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Usage: java TestAll <image-file>");
            System.exit(1);
        } catch (IIOException e) {
            System.out.println("Error reading image file " + srcFileName + " !");
            System.exit(1);
        }

        System.out.println("Source image: " + srcFileName);

        int width = image.getWidth();
        int height = image.getHeight();
        System.out.println("Image size is " + width + "x" + height);

        int[] sequentialSource = image.getRGB(0, 0, width, height, null, 0, width);

        int[] sequentialDst = new int[sequentialSource.length];

        executeSequentialFilter(sequentialSource, sequentialDst, width, height);

        System.out.println("\nAvailable processors: " + Runtime.getRuntime().availableProcessors());

        for (int i = 0; i < numberOfThreadsToUse.length; i++) {
            // Reset the source and destination
            int[] parallelSource = image.getRGB(0, 0, width, height, null, 0, width);
            int[] parallelDst = new int[sequentialSource.length];

            executeParallelFilter(parallelSource, parallelDst, width, height, numberOfThreadsToUse[i]);

            verifySolution(sequentialDst, parallelDst);

            showSpeedUp(speedUpValues[i]);

//            showNumberOfTasks();
//            ParallelFJImageFilter.numberOfTasksCreated = 0;
        }
    }

    private static void executeParallelFilter(int[] src, int[] dst, final int width, final int height, final int numberOfThreads) throws IOException {
        System.out.println("\nStarting parallel image filter using " + numberOfThreads + " threads.");

        ForkJoinPool pool = new ForkJoinPool(numberOfThreads);
        ParallelFJImageFilter filter1;
        final long startTime = System.currentTimeMillis();
        for (int steps = 0; steps < NRSTEPS; steps++) {
            filter1 = new ParallelFJImageFilter(src, dst, width, 1, height - 1);
            pool.invoke(filter1);

            // swap references
            int[] help;
            help = src;
            src = dst;
            dst = help;
        }
        final long endTime = System.currentTimeMillis();

        final long totalTime = endTime - startTime;

        System.out.println("Parallel image filter took " + totalTime + " milliseconds using " + numberOfThreads + " threads.");
        timeOfParallelExecution = totalTime;

        final BufferedImage dstImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        dstImage.setRGB(0, 0, width, height, dst, 0, width);

        final String filteredFileName = sourceFilePath.replace(sourceFileName, "ParallelFiltered" + sourceFileName);
        final File dstFile = new File(filteredFileName);

        ImageIO.write(dstImage, "jpg", dstFile);

        System.out.println("Output image for parallel filter: " + dstFile.getName());
    }

    private static void executeSequentialFilter(int[] src, int[] dst, final int width, final int height) throws IOException {
        System.out.println("\nStarting sequential image filter.");

        final long startTime = System.currentTimeMillis();
        final ImageFilter filter0 = new ImageFilter(src, dst, width, height);
        filter0.apply();
        final long endTime = System.currentTimeMillis();

        final long totalTime = endTime - startTime;
        System.out.println("Sequential image filter took " + totalTime + " milliseconds.");
        timeOfSequentialExecution = totalTime;

        BufferedImage dstImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        dstImage.setRGB(0, 0, width, height, dst, 0, width);

        String filteredFileName = sourceFilePath.replace(sourceFileName, "Filtered" + sourceFileName);
        final File dstFile = new File(filteredFileName);

        ImageIO.write(dstImage, "jpg", dstFile);

        System.out.println("Output image: " + dstFile.getName());
    }

    private static void showSpeedUp(float speedUpToCheck) {
        float speedUp = (float) timeOfSequentialExecution / timeOfParallelExecution;

        if (speedUpToCheck > 0f) {
            System.out.println("Speedup: " + speedUp + (speedUp >= speedUpToCheck ? "" : " not") + " ok (>= " + speedUpToCheck + ")");
        } else {
            System.out.println("Speedup: " + speedUp);
        }
    }

    private static void verifySolution(int[] source, int[] destination) {
        boolean parallelSolutionIsValid = verifyUsingTheDestinationArrays(source, destination);

        if (parallelSolutionIsValid)
            System.out.println("Output image verified successfully!");
        else
            System.out.println("Output image verification failed!");
    }

    private static boolean verifyUsingTheDestinationArrays(int[] source, int[] destination) {
        for (int i = 0; i < source.length; i++) {
            if (source[i] != destination[i]) {
                return false;
            }
        }

        return true;
    }

    private static void showNumberOfTasks() {
        System.out.println("Total number of tasks created: " + ParallelFJImageFilter.numberOfTasksCreated);
    }

    private static boolean verifyUsingImages(BufferedImage sequentialImage, BufferedImage parallelImage, int width, int height) {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (sequentialImage.getRGB(i, j) != parallelImage.getRGB(i, j))
                    return false;
            }
        }

        return true;
    }
}