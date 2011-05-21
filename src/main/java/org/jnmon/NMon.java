/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnmon;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.data.time.MovingAverage;
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
    lines = Files.readLines(filename, Charsets.UTF_8);
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

    final StandardXYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.LINES);
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
