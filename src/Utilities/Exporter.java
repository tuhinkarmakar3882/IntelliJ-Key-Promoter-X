package Utilities;

import de.halirutan.keypromoterx.statistic.StatisticsItem;
import com.opencsv.CSVWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Exporter {
  private final String basePath;
  private final String outputFileName;
  private final ArrayList<String[]> reportData;

  public Exporter(String basePath, String outputFileName, ArrayList<StatisticsItem> statisticsData) {
    this.basePath = basePath;
    this.outputFileName = outputFileName;
    this.reportData = preprocess(statisticsData);
  }

  public void export() throws IOException {
    createFilesAndDirectories();
    CSVWriter writer = new CSVWriter(new FileWriter(basePath + outputFileName));
    writer.writeAll(reportData);
    writer.flush();
  }

  // Todo Try using java.nio
  private void createFilesAndDirectories() throws IOException {
    new File(basePath).mkdirs();
    new File(basePath + outputFileName).createNewFile();
  }

  private ArrayList<String[]> preprocess(ArrayList<StatisticsItem> statisticsItems) {
    ArrayList<String[]> normalizedData = new ArrayList<>();
    normalizedData.add(new String[]{"shortcut", "description", "count", "ideaActionID"});
    for (StatisticsItem statisticsItem : statisticsItems) {
      normalizedData.add(statisticsItem.getStatisticalData());
    }
    return normalizedData;
  }
}
