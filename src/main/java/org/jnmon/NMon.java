/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnmon;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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
import java.util.zip.GZIPInputStream;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleEdge;

public class NMon {

  private String filepath;
  private List<String> lines;
  private Map<String, Date> snapshotTimes;
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
    snapshotTimes = new HashMap<String, Date>();

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

    final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(/* lines = */ true, /* shapes = */ false);
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

  public XYDataset createLPAR_CPU_vs_VP_DataSet() {
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
//VP_User%,VP_Sys%,VP_Wait%
    final TimeSeries s1 = new TimeSeries("VP_User%");
    final TimeSeries s2 = new TimeSeries("VP_Sys%");
    final TimeSeries s3 = new TimeSeries("VP_Wait%");

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

          s1.add(second, vp_user);
          s2.add(second, vp_sys);
          s3.add(second, vp_wait);
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

  public ChartPanel getLPAR2ChartPanel() {
    JFreeChart localJFreeChart = ChartFactory.createXYAreaChart("XY Area Chart Demo 2", "Time", "Value", createLPAR_CPU_vs_VP_DataSet(), PlotOrientation.VERTICAL, true, true, false);
    XYPlot localXYPlot = (XYPlot) localJFreeChart.getPlot();
    localXYPlot.setDomainPannable(true);
    DateAxis localDateAxis = new DateAxis("Time");
    localDateAxis.setLowerMargin(0.0D);
    localDateAxis.setUpperMargin(0.0D);
    localDateAxis.setVerticalTickLabels(true);
    localDateAxis.setTickUnit(new DateTickUnit(DateTickUnitType.HOUR, 1));
    localDateAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));
    localXYPlot.setDomainAxis(localDateAxis);
    //localXYPlot.setForegroundAlpha(0.5F);

    final ValueAxis rangeAxis = new NumberAxis("100 %");
    rangeAxis.setRange(0, 100);
    localXYPlot.setRangeAxis(rangeAxis);

    XYItemRenderer localXYItemRenderer = localXYPlot.getRenderer();
    localXYItemRenderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator("{0}: ({1}, {2})", new SimpleDateFormat("d-MMM-yyyy"), new DecimalFormat("#,##0.00")));


    //File file = new File("cpu.png");
    //ChartUtilities.saveChartAsPNG(file, chart, 800, 600);
    final ChartPanel chartPanel = new ChartPanel(localJFreeChart);
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
}
