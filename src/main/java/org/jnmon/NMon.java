/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnmon;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StackedXYAreaRenderer;
import org.jfree.chart.renderer.xy.StackedXYAreaRenderer2;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeTableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleEdge;

public class NMon {

  private String filepath;
  private List<String> lines;
  private SortedMap<String, Date> snapshotTimes = new TreeMap<String, Date>();
  private Map<String, List<String>> sections = new HashMap<String, List<String>>() {
  };

  public NMon(File filename) throws IOException, ParseException {
    this.filepath = filename.getCanonicalPath();
    if (filename.getName().endsWith(".gz")) {
      InputStream fileStream = new FileInputStream(filename);
      InputStream gzipStream = new GZIPInputStream(fileStream);
      Reader decoder = new InputStreamReader(gzipStream);
      BufferedReader bufferedReader = new BufferedReader(decoder);
      lines = com.google.common.io.CharStreams.readLines(bufferedReader);
      bufferedReader.close();
      decoder.close();
      gzipStream.close();
      fileStream.close();
    } else {
      lines = Files.readLines(filename, Charsets.UTF_8);
    }
    Collections.sort(lines);
    for (String line : lines) {
      String[] tokens = line.split(",", 2);
      String sectionName = tokens[0];
      List<String> section = sections.get(sectionName);
      if (section == null) {
        section = new ArrayList<String>();
        sections.put(sectionName, section);
      }
      section.add(line);
    }
    extractSnapshotTimes(getSection("ZZZZ"));
    insertDiskSum();
  }

  public List<String> getSections() {
    List<String> items = new ArrayList<String>();
    for (String section : sections.keySet()) {
      items.add(section);
    }
    Collections.sort(items);
    return items;
  }

  final public List<String> getSection(String section) {
    return sections.get(section);
  }

  private void extractSnapshotTimes(List<String> lines) throws ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss,dd-MMM-yyyy", Locale.US);
    sdf.setLenient(false);
    for (String line : lines) {
      String[] tokens = line.split(",", 2);
      if (tokens[0].equals("ZZZZ")) {
        String[] split = tokens[1].split(",", 2);
        String snapshotNumber = split[0];
        String time = split[1];
        Date date = sdf.parse(time);
        snapshotTimes.put(snapshotNumber, date);
      }
    }
  }

  public ChartPanel getLPARChartPanel() {
    final DateAxis domainAxis = new DateAxis("Time");
    domainAxis.setVerticalTickLabels(true);
    domainAxis.setTickUnit(new DateTickUnit(DateTickUnitType.HOUR, 1));
    domainAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));
    domainAxis.setLowerMargin(0.01);
    domainAxis.setUpperMargin(0.01);
    final ValueAxis rangeAxis = new NumberAxis("Number of active CPU");

    rangeAxis.setRange(0, getNumberOfActiveCPU());

    final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(/* lines = */true, /* shapes = */ false);
    final XYPlot plot = new XYPlot(createLPARDataSet(), domainAxis, rangeAxis, renderer);

    final JFreeChart chart = new JFreeChart("LPAR : " + filepath, plot);
    chart.getLegend().setPosition(RectangleEdge.TOP);

    //File file = new File("cpu.png");
    //ChartUtilities.saveChartAsPNG(file, chart, 800, 600);
    final ChartPanel chartPanel = new ChartPanel(chart);
    chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
    chartPanel.setMouseZoomable(true, false);

    return chartPanel;
  }

  public XYDataset createLPARDataSet() {
// LPAR,Logical Partition SCCDBDDR101P,PhysicalCPU,virtualCPUs,logicalCPUs,poolCPUs,entitled,weight,PoolIdle,usedAllCPU%,usedPoolCPU%,SharedCPU,Capped,EC_User%,EC_Sys%,EC_Wait%,EC_Idle%,VP_User%,VP_Sys%,VP_Wait%,VP_Idle%,Folded,Pool_id
// LPAR,T0001,0.872,4,8,8,0.55,192,0.00,10.91,10.91,1,0,85.89,69.80,0.17,2.77,11.81,9.60,0.02,0.38,0

    final TimeSeries s1 = new TimeSeries("PhysicalCPU");
    final TimeSeries s2 = new TimeSeries("entitled");
    final TimeSeries s3 = new TimeSeries("Unfolded VPs");

    boolean first = true;
    int i = 0;
    for (String line : lines) {
      String[] tokens = line.split(",", 2);
      if (tokens[0].equals("LPAR")) {
        if (first) {
          first = false;
        } else //if ((i % 10) == 0)
        {
          String items[] = tokens[1].split(",");

          String snapshot = items[0];
          Second second = new Second(snapshotTimes.get(snapshot));

          double physicalCPU = Double.valueOf(items[1]);
          double entitled = Double.valueOf(items[5]);
          double virtualCPUs = Double.valueOf(items[2]);
          double folded = Double.valueOf(items[20]);

          s1.add(second, physicalCPU);
          s2.add(second, entitled);
          s3.add(second, virtualCPUs - folded);
        }
        i++;
      }
    }
    final TimeSeriesCollection dataset = new TimeSeriesCollection();
    dataset.addSeries(s1);
    dataset.addSeries(s2);
    dataset.addSeries(s3);
    /*
     * dataset.addSeries(MovingAverage.createPointMovingAverage(s1, "AVG 20", 20));
    dataset.addSeries(MovingAverage.createPointMovingAverage(s1, "AVG 12", 12));
    dataset.addSeries(MovingAverage.createPointMovingAverage(s1, "AVG 10", 10));
    dataset.addSeries(MovingAverage.createPointMovingAverage(s1, "AVG 5", 5));
    dataset.addSeries(MovingAverage.createPointMovingAverage(s1, "AVG 3", 3));
     * 
     */
    //dataset.addSeries(MovingAverage.createMovingAverage(s1, "-AVG", 50));


    return dataset;
  }

  public TimeTableXYDataset createLPAR_CPU_vs_VP_DataSet() {
// LPAR,Logical Partition SCCDBDDR101P,PhysicalCPU,virtualCPUs,logicalCPUs,poolCPUs,entitled,weight,PoolIdle,usedAllCPU%,usedPoolCPU%,SharedCPU,Capped,EC_User%,EC_Sys%,EC_Wait%,EC_Idle%,VP_User%,VP_Sys%,VP_Wait%,VP_Idle%,Folded,Pool_id
// LPAR,T0001,0.872,4,8,8,0.55,192,0.00,10.91,10.91,1,0,85.89,69.80,0.17,2.77,11.81,9.60,0.02,0.38,0
//  1 - PhysicalCPU
//  2 - virtualCPUs
//  3 - logicalCPUs
//  4 - poolCPUs
//  5 - entitled
//  6 - weight
//  7 - PoolIdle
//  8 - usedAllCPU%
//  9 - usedPoolCPU%
// 10 - SharedCPU
// 11 - Capped
// 12 - EC_User%
// 13 - EC_Sys%
// 14 - EC_Wait%
// 15 - EC_Idle%
// 16 - VP_User%
// 17 - VP_Sys%
// 18 - VP_Wait%
// 19 - VP_Idle%
// 20 - Folded
// 21 - Pool_id

    TimeTableXYDataset localTimeTableXYDataset = new TimeTableXYDataset();

    boolean first = true;
    int i = 0;
    for (String line : lines) {
      String[] tokens = line.split(",", 2);
      if (tokens[0].equals("LPAR")) {
        if (first) {
          first = false;
        } else //if ((i % 10) == 0)
        {
          String items[] = tokens[1].split(",");

          String snapshot = items[0];
          Second second = new Second(snapshotTimes.get(snapshot));

          double vp_user = Double.valueOf(items[16]);
          double vp_sys = Double.valueOf(items[17]);
          double vp_wait = Double.valueOf(items[18]);

          localTimeTableXYDataset.add(second, vp_user, "VP_User%");
          localTimeTableXYDataset.add(second, vp_sys, "VP_Sys%");
          localTimeTableXYDataset.add(second, vp_wait, "VP_Wait%");
        }
        i++;
      }
    }
    return localTimeTableXYDataset;
  }

  public ChartPanel getLPAR2ChartPanel() {
    final DateAxis domainAxis = new DateAxis("Time");
    domainAxis.setVerticalTickLabels(true);
    domainAxis.setTickUnit(new DateTickUnit(DateTickUnitType.HOUR, 1));
    domainAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));
    domainAxis.setLowerMargin(0.01);
    domainAxis.setUpperMargin(0.01);
    final ValueAxis rangeAxis = new NumberAxis();

    // VP_User%, VP_Sys% and VP_Wait% are in %
    rangeAxis.setRange(0, 100);

    final StackedXYAreaRenderer renderer = new StackedXYAreaRenderer(StackedXYAreaRenderer.AREA);
    renderer.setOutline(false);
    renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator("{0}: ({1}, {2})", new SimpleDateFormat("HH:mm:ss"), new DecimalFormat("#,##0.00")));
    final XYPlot plot = new XYPlot(createLPAR_CPU_vs_VP_DataSet(), domainAxis, rangeAxis, renderer);
    // To avoid bug ID: 1225830 http://sourceforge.net/tracker/?func=detail&aid=1225830&group_id=15494&atid=115494
    plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);

    final JFreeChart chart = new JFreeChart("CPU% vs VPs", plot);
    chart.getLegend().setPosition(RectangleEdge.TOP);

    final ChartPanel chartPanel = new ChartPanel(chart);
    chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
    chartPanel.setMouseZoomable(true, false);

    return chartPanel;
  }

public ChartPanel getCPU_ALLChartPanel() {
    final DateAxis domainAxis = new DateAxis("Time");
    domainAxis.setVerticalTickLabels(true);
    domainAxis.setTickUnit(new DateTickUnit(DateTickUnitType.HOUR, 1));
    domainAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));
    domainAxis.setLowerMargin(0.01);
    domainAxis.setUpperMargin(0.01);
    final ValueAxis rangeAxis = new NumberAxis();

    final StackedXYAreaRenderer renderer = new StackedXYAreaRenderer(StackedXYAreaRenderer.AREA);
    renderer.setOutline(false);
    Color excelBlue = new Color(79, 129, 189);
    renderer.setSeriesPaint(0, excelBlue);
    renderer.setSeriesPaint(1, new Color(192, 80, 77));
    renderer.setSeriesPaint(2, new Color(155, 187, 89));
    renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator("{0}: ({1}, {2})", new SimpleDateFormat("HH:mm:ss"), new DecimalFormat("#,##0.00")));
    final XYPlot plot = new XYPlot(createCPU_ALL_DataSet(), domainAxis, rangeAxis, renderer);
    // To avoid bug ID: 1225830 http://sourceforge.net/tracker/?func=detail&aid=1225830&group_id=15494&atid=115494
    plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);

    final JFreeChart chart = new JFreeChart("CPU% vs VPs", plot);
    chart.getLegend().setPosition(RectangleEdge.TOP);

    final ChartPanel chartPanel = new ChartPanel(chart);
    chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
    chartPanel.setMouseZoomable(true, false);

    return chartPanel;
  }


// AAA,cpus,16,8
// cpus 		the number of CPUs in the system and the number active at the start of data collection.
  public int getNumberOfActiveCPU() {
    String numberOfActiveCPU = null;
    for (String line : lines) {
      if (line.startsWith("AAA,cpus")) {
        numberOfActiveCPU = line.split(",")[3];
        break;
      }
    }
    return Integer.valueOf(numberOfActiveCPU);

  }

  public Component getSharePoolUtilisation() {
    final DateAxis domainAxis = new DateAxis("Time");
    domainAxis.setVerticalTickLabels(true);
    domainAxis.setTickUnit(new DateTickUnit(DateTickUnitType.HOUR, 1));
    domainAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));
    domainAxis.setLowerMargin(0.01);
    domainAxis.setUpperMargin(0.01);
    final ValueAxis rangeAxis = new NumberAxis();


    rangeAxis.setRange(0, getNumberOfActiveCPU());

    final StackedXYAreaRenderer renderer = new StackedXYAreaRenderer(StackedXYAreaRenderer.AREA);
    renderer.setOutline(false);
    renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator("{0}: ({1}, {2})", new SimpleDateFormat("HH:mm:ss"), new DecimalFormat("#,##0.00")));
    final XYPlot plot = new XYPlot(createSharePoolUtilisationDataSet(), domainAxis, rangeAxis, renderer);
    // To avoid bug ID: 1225830 http://sourceforge.net/tracker/?func=detail&aid=1225830&group_id=15494&atid=115494
    plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);

    final JFreeChart chart = new JFreeChart("Share Pool Utilisation", plot);
    chart.getLegend().setPosition(RectangleEdge.TOP);

    final ChartPanel chartPanel = new ChartPanel(chart);
    chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
    chartPanel.setMouseZoomable(true, false);

    return chartPanel;
  }

  private TimeTableXYDataset createSharePoolUtilisationDataSet() {
// LPAR,Logical Partition SCCDBDDR101P,PhysicalCPU,virtualCPUs,logicalCPUs,poolCPUs,entitled,weight,PoolIdle,usedAllCPU%,usedPoolCPU%,SharedCPU,Capped,EC_User%,EC_Sys%,EC_Wait%,EC_Idle%,VP_User%,VP_Sys%,VP_Wait%,VP_Idle%,Folded,Pool_id
// LPAR,T0001,0.872,4,8,8,0.55,192,0.00,10.91,10.91,1,0,85.89,69.80,0.17,2.77,11.81,9.60,0.02,0.38,0
//  1 - PhysicalCPU
//  2 - virtualCPUs
//  3 - logicalCPUs
//  4 - poolCPUs
//  5 - entitled
//  6 - weight
//  7 - PoolIdle
//  8 - usedAllCPU%
//  9 - usedPoolCPU%
// 10 - SharedCPU
// 11 - Capped
// 12 - EC_User%
// 13 - EC_Sys%
// 14 - EC_Wait%
// 15 - EC_Idle%
// 16 - VP_User%
// 17 - VP_Sys%
// 18 - VP_Wait%
// 19 - VP_Idle%
// 20 - Folded
// 21 - Pool_id

    TimeTableXYDataset localTimeTableXYDataset = new TimeTableXYDataset();

    boolean first = true;
    int i = 0;
    for (String line : lines) {
      String[] tokens = line.split(",", 2);
      if (tokens[0].equals("LPAR")) {
        if (first) {
          first = false;
        } else //if ((i % 10) == 0)
        {
          String items[] = tokens[1].split(",");

          String snapshot = items[0];
          Second second = new Second(snapshotTimes.get(snapshot));

          double physical_CPU = Double.valueOf(items[1]);
          double poolCPUs = Double.valueOf(items[4]);
          double poolIdle = Double.valueOf(items[7]);

          localTimeTableXYDataset.add(second, physical_CPU, "PhysicalCPU");
          localTimeTableXYDataset.add(second, poolCPUs - physical_CPU, "OtherLPARs");
          localTimeTableXYDataset.add(second, poolIdle, "PoolIdle");
        }
        i++;
      }
    }
    return localTimeTableXYDataset;
  }

  public Component getMemoryRealFree() {
    final DateAxis domainAxis = new DateAxis("Time");
    domainAxis.setVerticalTickLabels(true);
    domainAxis.setTickUnit(new DateTickUnit(DateTickUnitType.HOUR, 1));
    domainAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));
    domainAxis.setLowerMargin(0.01);
    domainAxis.setUpperMargin(0.01);
    final ValueAxis rangeAxis = new NumberAxis();

    //rangeAxis.setRange(0, getNumberOfActiveCPU());

    final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(/* lines = */true, /* shapes = */ false);
    XYDataset dataSet = createMemoryRealFreeDataSet();
    for (int i = 0; i < dataSet.getSeriesCount(); i++) {
      renderer.setSeriesStroke(i, new BasicStroke(3));
    }

    final XYPlot plot = new XYPlot(dataSet, domainAxis, rangeAxis, renderer);

    final JFreeChart chart = new JFreeChart("Memory : " + filepath, plot);
    chart.getLegend().setPosition(RectangleEdge.TOP);

    //File file = new File("cpu.png");
    //ChartUtilities.saveChartAsPNG(file, chart, 800, 600);
    final ChartPanel chartPanel = new ChartPanel(chart);
    chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
    chartPanel.setMouseZoomable(true, false);

    return chartPanel;
  }

  private XYDataset createMemoryRealFreeDataSet() {
// MEM,Memory SVCDBATTR02Q,Real Free %,Virtual free %,Real free(MB),Virtual free(MB),Real total(MB),Virtual total(MB)
// MEM,T0001,63.7,99.9,5217.6,6136.1,8192.0,6144.0

    final TimeSeries s1 = new TimeSeries("Real free(MB)");
    final TimeSeries s2 = new TimeSeries("Virtual free(MB)");
    final TimeSeries s3 = new TimeSeries("Real total(MB)");
    final TimeSeries s4 = new TimeSeries("Virtual total(MB)");

    boolean first = true;
    int i = 0;
    for (String line : lines) {
      String[] tokens = line.split(",", 2);
      if (tokens[0].equals("MEM")) {
        if (first) {
          first = false;
        } else //if ((i % 10) == 0)
        {
          String items[] = tokens[1].split(",");

          String snapshot = items[0];
          Second second = new Second(snapshotTimes.get(snapshot));

          double realFree = Double.valueOf(items[3]);
          double virtualFree = Double.valueOf(items[4]);
          double realTotal = Double.valueOf(items[5]);
          double virtualTotal = Double.valueOf(items[6]);

          s1.add(second, realFree);
          s2.add(second, virtualFree);
          s3.add(second, realTotal);
          s4.add(second, virtualTotal);
        }
        i++;
      }
    }
    final TimeSeriesCollection dataset = new TimeSeriesCollection();
    dataset.addSeries(s1);
    dataset.addSeries(s2);
    dataset.addSeries(s3);
    dataset.addSeries(s4);

    return dataset;
  }

  private void insertDiskSum() {
    // DISKREAD,T0001,0.5,1130.2,3038.8,0.0,0.5
    // DISKWRITE,T0001,0.0,0.0,436.5,0.0,0.0
    List<String> diskReadSection = getSection("DISKREAD");
    List<String> diskWriteSection = getSection("DISKWRITE");
    List<String> diskIO = getSection("DISKXFER");

    if ((diskReadSection.size() == diskWriteSection.size())
            && (diskReadSection.size() == diskIO.size())) {
      ArrayList<String> diskSum = new ArrayList<String>();
      diskSum.add("DISK_SUMM,Disk total KB/s SVCDBATTR02Q,Disk Read KB/s,Disk Write KB/s,IO/sec");
      for (int i = 1; i < diskReadSection.size(); i++) {
        String read = diskReadSection.get(i);
        String write = diskWriteSection.get(i);
        String io = diskIO.get(i);
        
        String diskReadData[] = read.split(",");
        String diskWriteData[] = write.split(",");
        String ioData[] = io.split(","); 

        // Assert same snapshot time
        assert diskReadData[1].equals(diskWriteData[1]);
        assert diskReadData[1].equals(ioData[1]);

        BigDecimal diskReadSum = BigDecimal.ZERO;
        for (int j = 2; j < diskReadData.length; j++) {
          BigDecimal t = new BigDecimal(diskReadData[j]);
          diskReadSum = diskReadSum.add(new BigDecimal(diskReadData[j]));
        }

        BigDecimal diskWriteSum = BigDecimal.ZERO;
        for (int j = 2; j < diskWriteData.length; j++) {
          diskWriteSum = diskWriteSum.add(new BigDecimal(diskWriteData[j]));
        }

        BigDecimal ioSum = BigDecimal.ZERO;
        for (int j = 2; j < ioData.length; j++) {
          ioSum = ioSum.add(new BigDecimal(ioData[j]));
        }
        
        diskSum.add("DISK_SUMM," + diskReadData[1] + "," + diskReadSum + "," + diskWriteSum + "," + ioSum);
      }
      sections.put("DISK_SUMM", diskSum);
    }
  }

  private XYDataset createCPU_ALL_DataSet() {
    // CPU_ALL,CPU Total lr102ora3502c,User%,Sys%,Wait%,Idle%,Busy,CPUs
    // CPU_ALL,T0043,12.5,8.1,0.8,78.6,,2
    // 1 - User%
    // 2 - Sys%
    // 3 - Wait%
    // 4 - Idle%
    // 5 - Busy
    // 6 - CPUs
 
    TimeTableXYDataset localTimeTableXYDataset = new TimeTableXYDataset();

    boolean first = true;
    int i = 0;
    for (String line : lines) {
      String[] tokens = line.split(",", 2);
      if (tokens[0].equals("CPU_ALL")) {
        if (first) {
          first = false;
        } else //if ((i % 10) == 0)
        {
          String items[] = tokens[1].split(",");

          String snapshot = items[0];
          Second second = new Second(snapshotTimes.get(snapshot));

          double user_percent = Double.valueOf(items[1]);
          double sys_percent = Double.valueOf(items[2]);
          double wait_percent = Double.valueOf(items[3]);

          localTimeTableXYDataset.add(second, user_percent, "User%");
          localTimeTableXYDataset.add(second, sys_percent, "Sys%");
          localTimeTableXYDataset.add(second, wait_percent, "Wait%");
        }
        i++;
      }
    }
    return localTimeTableXYDataset;
  }
}
