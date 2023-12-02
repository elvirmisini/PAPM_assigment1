import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class ParallelFJImageFilter {
    private int[] src;
    private int[] dst;
    private int width;
    private int height;
    private final int NRSTEPS = 100;

    public ParallelFJImageFilter(int[] src, int[] dst, int w, int h) {
        this.src = src;
        this.dst = dst;
        this.width = w;
        this.height = h;
    }

    public void apply(int nthreads) {
        ParallelFilterTask mainTask = new ParallelFilterTask(src, dst, width, height, 0, height - 1, nthreads);
         ForkJoinPool.commonPool().invoke(mainTask);
    }


    private class ParallelFilterTask extends RecursiveAction {
        private int[] src;
        private int[] dst;
        private int width;
        private int height;
        private int startRow;
        private int endRow;
        private int nthreads;

        private ParallelFilterTask(int[] src, int[] dst, int width, int height, int startRow, int endRow, int nthreads) {
            this.src = src;
            this.dst = dst;
            this.width = width;
            this.height = height;
            this.startRow = startRow;
            this.endRow = endRow;
            this.nthreads = nthreads;
        }

        @Override
        protected void compute() {
            if ((endRow - startRow + 1) <= height / nthreads) {
                // Sequential computation for small tasks
                sequentialFilter(startRow, endRow);
            } else {
                // Divide the task into smaller subtasks
                int middleRow = (startRow + endRow) / 2;
                ParallelFilterTask leftTask = new ParallelFilterTask(src, dst, width, height, startRow, middleRow, nthreads);
                ParallelFilterTask rightTask = new ParallelFilterTask(src, dst, width, height, middleRow + 1, endRow, nthreads);
                invokeAll(leftTask, rightTask);
            }
        }

        private void sequentialFilter(int startRow, int endRow) {
            int index, pixel;
		// for (int steps = 0; steps < NRSTEPS; steps++) {
		// 	for (int i = startRow; i < endRow - 1; i++) {
		// 		for (int j = 1; j < width - 1; j++) {
		// 			float rt = 0, gt = 0, bt = 0;
		// 			for (int k = i - 1; k <= i + 1; k++) {
		// 				index = k * width + j - 1;
		// 				pixel = src[index];
		// 				rt += (float) ((pixel & 0x00ff0000) >> 16);
		// 				gt += (float) ((pixel & 0x0000ff00) >> 8);
		// 				bt += (float) ((pixel & 0x000000ff));

		// 				index = k * width + j;
		// 				pixel = src[index];
		// 				rt += (float) ((pixel & 0x00ff0000) >> 16);
		// 				gt += (float) ((pixel & 0x0000ff00) >> 8);
		// 				bt += (float) ((pixel & 0x000000ff));

		// 				index = k * width + j + 1;
		// 				pixel = src[index];
		// 				rt += (float) ((pixel & 0x00ff0000) >> 16);
		// 				gt += (float) ((pixel & 0x0000ff00) >> 8);
		// 				bt += (float) ((pixel & 0x000000ff));
		// 			}
		// 			// Re-assemble destination pixel.
		// 			index = i * width + j;
		// 			int dpixel = (0xff000000) | (((int) rt / 9) << 16) | (((int) gt / 9) << 8) | (((int) bt / 9));
		// 			dst[index] = dpixel;
		// 		}
		// 	}
		// 	// swap references
		// 	int[] help; help = src; src = dst; dst = help;
		// }
        }
    }
}