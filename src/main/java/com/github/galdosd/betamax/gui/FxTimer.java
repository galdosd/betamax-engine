package com.github.galdosd.betamax.gui;

import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import javafx.beans.property.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * FIXME: Document this class
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class FxTimer extends FxRow<String> {
    @IgnoreColumn
    final static int MS_PER_NS = 1000000;
    @ColumnWidth(200)
    SimpleStringProperty name = new SimpleStringProperty();
    SimpleStringProperty mean = new SimpleStringProperty();
    SimpleLongProperty count = new SimpleLongProperty();
    SimpleStringProperty rate1m = new SimpleStringProperty();
    SimpleStringProperty minimum = new SimpleStringProperty();
    SimpleStringProperty pct5 = new SimpleStringProperty();
    SimpleStringProperty pct25 = new SimpleStringProperty();
    SimpleStringProperty median = new SimpleStringProperty();
    SimpleStringProperty pct75 = new SimpleStringProperty();
    SimpleStringProperty pct95 = new SimpleStringProperty();
    SimpleStringProperty maximum = new SimpleStringProperty();

    public FxTimer(int tableIndex, String name, Timer timer) {
        this.tableIndex = tableIndex;
        Snapshot snapshot = timer.getSnapshot();
        setName(name.replace("Timer",""));
        setCount(timer.getCount());
        setRate1m(String.format("%.2f", timer.getOneMinuteRate()));
        setMean(formatFloat(snapshot.getMean()));
        setMinimum(formatFloat(snapshot.getMin()));
        setPct5(formatFloat(snapshot.getValue(0.05)));
        setPct25(formatFloat(snapshot.getValue(0.25)));
        setMedian(formatFloat(snapshot.getMedian()));
        setPct75(formatFloat(snapshot.get75thPercentile()));
        setPct95(formatFloat(snapshot.get95thPercentile()));
        setMaximum(formatFloat(snapshot.getMax()));
    }

    private String formatFloat(double val) {
        return String.format("%.1f", val / MS_PER_NS);
    }

    @Override public String getID() {
        return getName();
    }

    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }

    public long getCount() { return count.get(); }
    public void setCount(long count) { this.count.set(count); }

    public String getRate1m() { return rate1m.get(); }
    public void setRate1m(String rate1m) { this.rate1m.set(rate1m); }

    public String getMedian() { return median.get(); }
    public void setMedian(String median) { this.median.set(median); }

    public String getMean() { return mean.get(); }
    public void setMean(String mean) { this.mean.set(mean); }

    public String getMinimum() { return minimum.get(); }
    public void setMinimum(String minimum) { this.minimum.set(minimum); }

    public String getMaximum() { return maximum.get(); }
    public void setMaximum(String maximum) { this.maximum.set(maximum); }

    public String getPct95() { return pct95.get(); }
    public void setPct95(String pct95) { this.pct95.set(pct95); }

    public String getPct5() { return pct5.get(); }
    public void setPct5(String pct5) { this.pct5.set(pct5); }

    public String getPct25() { return pct25.get(); }
    public void setPct25(String pct25) { this.pct25.set(pct25); }

    public String getPct75() { return pct75.get(); }
    public void setPct75(String pct75) { this.pct75.set(pct75); }
}
