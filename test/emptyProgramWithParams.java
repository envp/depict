public class emptyProgramWithParams implements Runnable {
  // Instance vars generated from List<ParamDec>
  int i;
  boolean b;
  int i2;
  boolean b2;

  // Constructor
  public emptyProgramWithParams(String[] args) {
    // TODO: Initialize with values from args
    this.i = Integer.parseInt(args[0]);
    this.b = Boolean.parseBoolean(args[1]);
    this.i2 = Integer.parseInt(args[2]);
    this.b2 = Boolean.parseBoolean(args[3]);
  }

  public static void main(String[] args) {
    emptyProgramWithParams instance = new emptyProgramWithParams(args);
    instance.run();
  }

  public void run() {
    // declaration list
    // statement list
  }
}
