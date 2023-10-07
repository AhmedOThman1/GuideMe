package com.guideMe.deepmodel;

import android.graphics.RectF;

public final class DetectionResult implements Comparable<DetectionResult> {
    private final Integer id;
    private final String title;
    private final Float confidence;
    private RectF location;

    public DetectionResult(final Integer id, final String title,
                           final Float confidence, final RectF location) {
        this.id = id;
        this.title = title;
        this.confidence = confidence;
        this.location = location;
    }

    public DetectionResult(Integer id, String title, Float confidence) {
        this.id = id;
        this.title = title;
        this.confidence = confidence;
    }

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Float getConfidence() {
        return confidence;
    }

    public RectF getLocation() {
        return new RectF(location);
    }

    public void setLocation(RectF location) {
        this.location = location;
    }

    @Override
    public int compareTo(DetectionResult other) {
        return Float.compare(other.getConfidence(), this.getConfidence());
    }

    @Override
    public String toString() {
        return title+" -> "+confidence;
        /*"DetectionResult{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", confidence=" + confidence +
                ", location=" + location +
                '}';
    */
    }
}
