import java.util.Map;

import javax.swing.JPanel;

import com.jogamp.opengl.GL2;

/**
 * Renders a time-domain line chart.
 * 
 * User settings:
 *     Datasets to visualize.
 *     Sample count.
 *     Y-axis minimum value can be fixed or autoscaled.
 *     Y-axis maximum value can be fixed or autoscaled.
 *     X-axis title can be displayed.
 *     X-axis scale can be displayed.
 *     Y-axis title can be displayed.
 *     Y-axis scale can be displayed.
 *     Legend can be displayed.
 */
@SuppressWarnings("serial")
public class OpenGLTimeDomainChart extends PositionedChart {
	
	OpenGLTimeDomainSlice slice;
	
	// plot region
	float xPlotLeft;
	float xPlotRight;
	float plotWidth;
	float yPlotTop;
	float yPlotBottom;
	float plotHeight;
	
	// x-axis title
	boolean showXaxisTitle;
	float yXaxisTitleTextBasline;
	float yXaxisTitleTextTop;
	String xAxisTitle;
	float xXaxisTitleTextLeft;
	
	// legend
	boolean showLegend;
	float xLegendBorderLeft;
	float yLegendBorderBottom;
	float yLegendTextBaseline;
	float yLegendTextTop;
	float yLegendBorderTop;
	float[][] legendBoxCoordinates;
	float[] xLegendNameLeft;
	float xLegendBorderRight;
	
	// x-axis scale
	boolean showXaxisScale;
	Map<Integer, String> xDivisions;
	float yXaxisTickTextBaseline;
	float yXaxisTickTextTop;
	float yXaxisTickBottom;
	float yXaxisTickTop;
	
	// y-axis title
	boolean showYaxisTitle;
	float xYaxisTitleTextTop;
	float xYaxisTitleTextBaseline;
	String yAxisTitle;
	float yYaxisTitleTextLeft;
	
	// y-axis scale
	boolean showYaxisScale;
	Map<Float, String> yDivisions;
	float xYaxisTickTextRight;
	float xYaxisTickLeft;
	float xYaxisTickRight;
	AutoScale autoscale;
	boolean autoscaleMin;
	boolean autoscaleMax;
	float manualMin;
	float manualMax;
	
	public static ChartFactory getFactory() {
		
		return new ChartFactory() {
			
			WidgetDatasets datasetsWidget;
			WidgetTextfieldInteger sampleCountWidget;
			WidgetTextfieldsOptionalMinMax minMaxWidget;
			WidgetCheckbox showXaxisTitleWidget;
			WidgetCheckbox showXaxisScaleWidget;
			WidgetCheckbox showYaxisTitleWidget;
			WidgetCheckbox showYaxisScaleWidget;
			WidgetCheckbox showLegendWidget;
			
			@Override public String toString() { return "Time Domain Chart"; }
			
			@Override public JPanel[] getWidgets() {

				datasetsWidget       = new WidgetDatasets();
				sampleCountWidget    = new WidgetTextfieldInteger("Sample Count", 1000, 5, Integer.MAX_VALUE);
				minMaxWidget         = new WidgetTextfieldsOptionalMinMax("Y-Axis", -1.0f, 1.0f, -Float.MAX_VALUE, Float.MAX_VALUE);
				showXaxisTitleWidget = new WidgetCheckbox("Show X-Axis Title", true);
				showXaxisScaleWidget = new WidgetCheckbox("Show X-Axis Scale", true);
				showYaxisTitleWidget = new WidgetCheckbox("Show Y-Axis Title", true);
				showYaxisScaleWidget = new WidgetCheckbox("Show Y-Axis Scale", true);
				showLegendWidget     = new WidgetCheckbox("Show Legend", true);
	
				JPanel[] widgets = new JPanel[13];
				
				widgets[0]  = datasetsWidget;
				widgets[1]  = null;
				widgets[2]  = sampleCountWidget;
				widgets[3]  = null;
				widgets[4]  = minMaxWidget;
				widgets[5]  = null;
				widgets[6]  = showXaxisTitleWidget;
				widgets[7]  = showXaxisScaleWidget;
				widgets[8]  = null;
				widgets[9]  = showYaxisTitleWidget;
				widgets[10] = showYaxisScaleWidget;
				widgets[11] = null;
				widgets[12] = showLegendWidget;

				return widgets;
				
			}
			
			@Override public int getMinimumSampleCount() {
				return 5;
			}
			
			@Override public PositionedChart createChart(int x1, int y1, int x2, int y2) {

				int sampleCount        = sampleCountWidget.getValue();
				Dataset[] datasets     = datasetsWidget.getDatasets();
				boolean autoscaleMin   = minMaxWidget.isMinimumAutomatic();
				float manualMin        = minMaxWidget.getMinimumValue();
				boolean autoscaleMax   = minMaxWidget.isMaximumAutomatic();
				float manualMax        = minMaxWidget.getMaximumValue();
				boolean showXaxisTitle = showXaxisTitleWidget.isChecked();
				boolean showXaxisScale = showXaxisScaleWidget.isChecked();
				boolean showYaxisTitle = showYaxisTitleWidget.isChecked();
				boolean showYaxisScale = showYaxisScaleWidget.isChecked();
				boolean showLegend     = showLegendWidget.isChecked();
				
				if(datasets.length == 0)
					return null;
				
				OpenGLTimeDomainChart chart = new OpenGLTimeDomainChart(x1, y1, x2, y2, sampleCount, datasets);
				chart.setYaxisRange(autoscaleMin, manualMin, autoscaleMax, manualMax);
				chart.setVisibleRegions(showXaxisTitle, showXaxisScale, showYaxisTitle, showYaxisScale, showLegend);
				
				return chart;
				
			}
			
			@Override public PositionedChart importChart(int x1, int y1, int x2, int y2, Dataset[] datasets, int sampleCount, String[] lines, int firstLineNumber) {
				
				if(lines.length != 9)
					throw new AssertionError("Line " + firstLineNumber + ": Invalid Time Domain Chart configuration section.");
				
				boolean autoscaleMin   = (boolean) ChartUtils.parse(firstLineNumber + 0, lines[0], "autoscale minimum = %b");
				float manualMin        =   (float) ChartUtils.parse(firstLineNumber + 1, lines[1], "manual minimum = %f");
				boolean autoscaleMax   = (boolean) ChartUtils.parse(firstLineNumber + 2, lines[2], "autoscale maximum = %b");
				float manualMax        =   (float) ChartUtils.parse(firstLineNumber + 3, lines[3], "manual maximum = %f");
				boolean showXaxisTitle = (boolean) ChartUtils.parse(firstLineNumber + 4, lines[4], "show x-axis title = %b");
				boolean showXaxisScale = (boolean) ChartUtils.parse(firstLineNumber + 5, lines[5], "show x-axis scale = %b");
				boolean showYaxisTitle = (boolean) ChartUtils.parse(firstLineNumber + 6, lines[6], "show y-axis title = %b");
				boolean showYaxisScale = (boolean) ChartUtils.parse(firstLineNumber + 7, lines[7], "show y-axis scale = %b");
				boolean showLegend     = (boolean) ChartUtils.parse(firstLineNumber + 8, lines[8], "show legend = %b");
				
				OpenGLTimeDomainChart chart = new OpenGLTimeDomainChart(x1, y1, x2, y2, sampleCount, datasets);
				chart.setYaxisRange(autoscaleMin, manualMin, autoscaleMax, manualMax);
				chart.setVisibleRegions(showXaxisTitle, showXaxisScale, showYaxisTitle, showYaxisScale, showLegend);
				
				return chart;
				
			}
			
		};
		
	}
	
	@Override public String[] exportChartSettings() {
		
		String[] lines = new String[9];
		
		lines[0] = "autoscale minimum = " + autoscaleMin;
		lines[1] = "manual minimum = " + manualMin;
		lines[2] = "autoscale maximum = " + autoscaleMax;
		lines[3] = "manual maximum = " + manualMax;
		lines[4] = "show x-axis title = " + showXaxisTitle;
		lines[5] = "show x-axis scale = " + showXaxisScale;
		lines[6] = "show y-axis title = " + showYaxisTitle;
		lines[7] = "show y-axis scale = " + showYaxisScale;
		lines[8] = "show legend = " + showLegend;
		
		return lines;
		
	}
	
	@Override public String toString() {
		
		return "Time Domain Chart";
		
	}
	
	public OpenGLTimeDomainChart(int x1, int y1, int x2, int y2, int chartDuration, Dataset[] chartInputs) {
		
		super(x1, y1, x2, y2, chartDuration, chartInputs);
		
		autoscale = new AutoScale(AutoScale.MODE_EXPONENTIAL, 30, 0.10f);
		autoscaleMin = true;
		autoscaleMax = true;
		
		showXaxisTitle = true;
		showXaxisScale = true;
		showYaxisTitle = true;
		showYaxisScale = true;
		showLegend = true;

	}
	
	public void setYaxisRange(boolean autoscaleMinimum, float manualMinimum, boolean autoscaleMaximum, float manualMaximum) {
		
		autoscaleMin = autoscaleMinimum;
		autoscaleMax = autoscaleMaximum;
		manualMin = manualMinimum;
		manualMax = manualMaximum;
		
	}
	
	public void setVisibleRegions(boolean showXaxisTitle, boolean showXaxisScale, boolean showYaxisTitle, boolean showYaxisScale, boolean showLegend) {
		
		this.showXaxisTitle = showXaxisTitle;
		this.showXaxisScale = showXaxisScale;
		this.showYaxisTitle = showYaxisTitle;
		this.showYaxisScale = showYaxisScale;
		this.showLegend     = showLegend;
		
	}
	
	@Override public void drawChart(GL2 gl, int width, int height, int lastSampleNumber, double zoomLevel) {
		
		// calculate domain
		int plotMaxX = lastSampleNumber;
		int plotMinX = plotMaxX - (int) (duration * zoomLevel) + 1;
		int minDomain = OpenGLTimeDomainChart.getFactory().getMinimumSampleCount() - 1;
		if(plotMaxX - plotMinX < minDomain) plotMinX = plotMaxX - minDomain;
		float domain = plotMaxX - plotMinX;
		
		if(plotMaxX < minDomain)
			return;
		
		// get the samples
		if(slice == null) {
			slice = new OpenGLTimeDomainSlice(gl);
			slice.freeResources(gl);
		}
		slice.updateSamples(datasets, plotMinX > 0 ? plotMinX : 0, plotMaxX);
		
		// ensure range is >0
		float plotMaxY = slice.sliceMaxY;
		float plotMinY = slice.sliceMinY;
		if(plotMinY == plotMaxY) {
			float value = plotMinY;
			plotMinY = value - 0.001f;
			plotMaxY = value + 0.001f;
		}
		autoscale.update(plotMinY, plotMaxY);
		plotMaxY = autoscaleMax ? autoscale.getMax() : manualMax;
		plotMinY = autoscaleMin ? autoscale.getMin() : manualMin;
		float plotRange = plotMaxY - plotMinY;
		
		// calculate x and y positions of everything
		xPlotLeft = Theme.tilePadding;
		xPlotRight = width - Theme.tilePadding;
		plotWidth = xPlotRight - xPlotLeft;
		yPlotTop = height - Theme.tilePadding;
		yPlotBottom = Theme.tilePadding;
		plotHeight = yPlotTop - yPlotBottom;
		
		if(showXaxisTitle) {
			yXaxisTitleTextBasline = Theme.tilePadding;
			yXaxisTitleTextTop = yXaxisTitleTextBasline + FontUtils.xAxisTextHeight;
			xAxisTitle = "Sample Number";
			xXaxisTitleTextLeft = xPlotLeft + (plotWidth  / 2.0f) - (FontUtils.xAxisTextWidth(xAxisTitle)  / 2.0f);
			
			float temp = yXaxisTitleTextTop + Theme.tickTextPadding;
			if(yPlotBottom < temp) {
				yPlotBottom = temp;
				plotHeight = yPlotTop - yPlotBottom;
			}
		}
		
		if(showLegend) {
			xLegendBorderLeft = Theme.tilePadding;
			yLegendBorderBottom = Theme.tilePadding;
			yLegendTextBaseline = yLegendBorderBottom + Theme.legendTextPadding;
			yLegendTextTop = yLegendTextBaseline + FontUtils.legendTextHeight;
			yLegendBorderTop = yLegendTextTop + Theme.legendTextPadding;
			
			legendBoxCoordinates = new float[datasets.length][8];
			xLegendNameLeft = new float[datasets.length];
			
			float xOffset = xLegendBorderLeft + (Theme.lineWidth / 2) + Theme.legendTextPadding;
			
			for(int i = 0; i < datasets.length; i++){
				legendBoxCoordinates[i][0] = xOffset;
				legendBoxCoordinates[i][1] = yLegendTextBaseline;
				
				legendBoxCoordinates[i][2] = xOffset;
				legendBoxCoordinates[i][3] = yLegendTextTop;
				
				legendBoxCoordinates[i][4] = xOffset + FontUtils.legendTextHeight;
				legendBoxCoordinates[i][5] = yLegendTextTop;
				
				legendBoxCoordinates[i][6] = xOffset + FontUtils.legendTextHeight;
				legendBoxCoordinates[i][7] = yLegendTextBaseline;
				
				xOffset += FontUtils.legendTextHeight + Theme.legendTextPadding;
				xLegendNameLeft[i] = xOffset;
				xOffset += FontUtils.legendTextWidth(datasets[i].name) + Theme.legendNamesPadding;
			}
			
			xLegendBorderRight = xOffset - Theme.legendNamesPadding + Theme.legendTextPadding + (Theme.lineWidth / 2);
			if(showXaxisTitle)
				xXaxisTitleTextLeft = xLegendBorderRight + ((xPlotRight - xLegendBorderRight) / 2) - (FontUtils.xAxisTextWidth(xAxisTitle)  / 2.0f);
			
			float temp = yLegendBorderTop + Theme.legendTextPadding;
			if(yPlotBottom < temp) {
				yPlotBottom = temp;
				plotHeight = yPlotTop - yPlotBottom;
			}
		}
		
		if(showXaxisScale) {
			yXaxisTickTextBaseline = yPlotBottom;
			yXaxisTickTextTop = yXaxisTickTextBaseline + FontUtils.tickTextHeight;
			yXaxisTickBottom = yXaxisTickTextTop + Theme.tickTextPadding;
			yXaxisTickTop = yXaxisTickBottom + Theme.tickLength;
			
			yPlotBottom = yXaxisTickTop;
			plotHeight = yPlotTop - yPlotBottom;
		}
		
		if(showYaxisTitle) {
			xYaxisTitleTextTop = xPlotLeft;
			xYaxisTitleTextBaseline = xYaxisTitleTextTop + FontUtils.yAxisTextHeight;
			yAxisTitle = datasets[0].unit;
			yYaxisTitleTextLeft = yPlotBottom + (plotHeight / 2.0f) - (FontUtils.yAxisTextWidth(yAxisTitle) / 2.0f);
			
			xPlotLeft = xYaxisTitleTextBaseline + Theme.tickTextPadding;
			plotWidth = xPlotRight - xPlotLeft;
			
			if(showXaxisTitle && !showLegend)
				xXaxisTitleTextLeft = xPlotLeft + (plotWidth  / 2.0f) - (FontUtils.xAxisTextWidth(xAxisTitle)  / 2.0f);
		}
		
		if(showYaxisScale) {
			yDivisions = ChartUtils.getYdivisions125(plotHeight, plotMinY, plotMaxY);
			float maxTextWidth = 0;
			for(String text : yDivisions.values()) {
				float textWidth = FontUtils.tickTextWidth(text);
				if(textWidth > maxTextWidth)
					maxTextWidth = textWidth;
					
			}
			
			xYaxisTickTextRight = xPlotLeft + maxTextWidth;
			xYaxisTickLeft = xYaxisTickTextRight + Theme.tickTextPadding;
			xYaxisTickRight = xYaxisTickLeft + Theme.tickLength;
			
			xPlotLeft = xYaxisTickRight;
			plotWidth = xPlotRight - xPlotLeft;
			
			if(showXaxisTitle && !showLegend)
				xXaxisTitleTextLeft = xPlotLeft + (plotWidth  / 2.0f) - (FontUtils.xAxisTextWidth(xAxisTitle)  / 2.0f);
		}
	
		// get the x divisions now that we know the final plot width
		xDivisions = ChartUtils.getXdivisions125(plotWidth, plotMinX, plotMaxX);
		
		// stop if the plot is too small
		if(plotWidth < 1 || plotHeight < 1)
			return;
		
		// draw plot background
		gl.glBegin(GL2.GL_QUADS);
		gl.glColor4fv(Theme.plotBackgroundColor, 0);
			gl.glVertex2f(xPlotLeft,  yPlotTop);
			gl.glVertex2f(xPlotRight, yPlotTop);
			gl.glVertex2f(xPlotRight, yPlotBottom);
			gl.glVertex2f(xPlotLeft,  yPlotBottom);
		gl.glEnd();
		
		// draw the x-axis scale
		if(showXaxisScale) {
			gl.glBegin(GL2.GL_LINES);
			for(Integer xValue : xDivisions.keySet()) {
				float x = (float) (xValue - plotMinX) / domain * plotWidth + xPlotLeft;
				gl.glColor4fv(Theme.divisionLinesColor, 0);
				gl.glVertex2f(x, yPlotTop);
				gl.glVertex2f(x, yPlotBottom);
				gl.glColor4fv(Theme.tickLinesColor, 0);
				gl.glVertex2f(x, yXaxisTickTop);
				gl.glVertex2f(x, yXaxisTickBottom);
			}
			gl.glEnd();
			
			for(Map.Entry<Integer,String> entry : xDivisions.entrySet()) {
				float x = (float) (entry.getKey() - plotMinX) / domain * plotWidth + xPlotLeft - (FontUtils.tickTextWidth(entry.getValue()) / 2.0f);
				float y = yXaxisTickTextBaseline;
				FontUtils.drawTickText(entry.getValue(), (int) x, (int) y);
			}
		}
		
		// draw the y-axis scale
		if(showYaxisScale) {
			gl.glBegin(GL2.GL_LINES);
			for(Float entry : yDivisions.keySet()) {
				float y = (entry - plotMinY) / plotRange * plotHeight + yPlotBottom;
				gl.glColor4fv(Theme.divisionLinesColor, 0);
				gl.glVertex2f(xPlotLeft,  y);
				gl.glVertex2f(xPlotRight, y);
				gl.glColor4fv(Theme.tickLinesColor, 0);
				gl.glVertex2f(xYaxisTickLeft,  y);
				gl.glVertex2f(xYaxisTickRight, y);
			}
			gl.glEnd();
			
			for(Map.Entry<Float,String> entry : yDivisions.entrySet()) {
				float x = xYaxisTickTextRight - FontUtils.tickTextWidth(entry.getValue());
				float y = (entry.getKey() - plotMinY) / plotRange * plotHeight + yPlotBottom - (FontUtils.tickTextHeight / 2.0f);
				FontUtils.drawTickText(entry.getValue(), (int) x, (int) y);
			}
		}
		
		// draw the legend, if space is available
		if(showLegend && xLegendBorderRight < width - Theme.tilePadding) {
			gl.glBegin(GL2.GL_LINE_LOOP);
			gl.glColor4fv(Theme.legendOutlineColor, 0);
				gl.glVertex2f(xLegendBorderLeft,  yLegendBorderBottom);
				gl.glVertex2f(xLegendBorderLeft,  yLegendBorderTop);
				gl.glVertex2f(xLegendBorderRight, yLegendBorderTop);
				gl.glVertex2f(xLegendBorderRight, yLegendBorderBottom);
			gl.glEnd();
			
			for(int i = 0; i < datasets.length; i++) {
				gl.glBegin(GL2.GL_QUADS);
				gl.glColor4fv(slice.glDataset[i].color, 0);
					gl.glVertex2f(legendBoxCoordinates[i][0], legendBoxCoordinates[i][1]);
					gl.glVertex2f(legendBoxCoordinates[i][2], legendBoxCoordinates[i][3]);
					gl.glVertex2f(legendBoxCoordinates[i][4], legendBoxCoordinates[i][5]);
					gl.glVertex2f(legendBoxCoordinates[i][6], legendBoxCoordinates[i][7]);
				gl.glEnd();
				
				FontUtils.drawLegendText(slice.glDataset[i].name, (int) xLegendNameLeft[i], (int) yLegendTextBaseline);
			}
		}
		
		// draw the x-axis title, if space is available
		if(showXaxisTitle)
			if((!showLegend && xXaxisTitleTextLeft > xPlotLeft) || (showLegend && xXaxisTitleTextLeft > xLegendBorderRight + Theme.legendTextPadding))
				FontUtils.drawXaxisText(xAxisTitle, (int) xXaxisTitleTextLeft, (int) yXaxisTitleTextBasline);
		
		// draw the y-axis title, if space is available
		if(showYaxisTitle && yYaxisTitleTextLeft > yPlotBottom)
			FontUtils.drawYaxisText(yAxisTitle, (int) xYaxisTitleTextBaseline, (int) yYaxisTitleTextLeft, 90);
		
		// clip to the plot region
		int[] originalScissorArgs = new int[4];
		gl.glGetIntegerv(GL2.GL_SCISSOR_BOX, originalScissorArgs, 0);
		gl.glScissor(originalScissorArgs[0] + (int) xPlotLeft, originalScissorArgs[1] + (int) yPlotBottom, (int) plotWidth, (int) plotHeight);
		
		// draw each dataset
		if(slice.glDataset[0].vertexCount >= 2) {
			for(int i = 0; i < slice.glDataset.length; i++) {
				
				gl.glMatrixMode(GL2.GL_MODELVIEW);
				gl.glPushMatrix();
				
				// adjust so: x = (x - plotMinX) / domain * plotWidth + xPlotLeft;
				gl.glTranslatef(xPlotLeft, 0, 0);
				gl.glScalef(plotWidth, 1, 1);
				gl.glScalef(1.0f / domain, 1, 1);
				gl.glTranslatef(-plotMinX, 0, 0);
				
				// adjust so y = (y - plotMinY) / plotRange * plotHeight + yPlotBottom;
				gl.glTranslatef(0, yPlotBottom, 0);
				gl.glScalef(1, plotHeight, 1);
				gl.glScalef(1, 1.0f / plotRange, 1);
				gl.glTranslatef(0, -plotMinY, 0);
				
				gl.glColor4fv(slice.glDataset[i].color, 0);
				gl.glVertexPointer(2, GL2.GL_FLOAT, 0, slice.glDataset[i].buffer);
				gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, slice.glDataset[i].vertexCount);
				
				// also draw points if there are relatively few samples on screen
				if(plotWidth / domain > 2 * Theme.pointSize)
					gl.glDrawArrays(GL2.GL_POINTS, 0, slice.glDataset[i].vertexCount);
				
				gl.glPopMatrix();
				
			}
		}

		// stop clipping to the plot region
		gl.glScissor(originalScissorArgs[0], originalScissorArgs[1], originalScissorArgs[2], originalScissorArgs[3]);
		
		// draw the plot border
		gl.glBegin(GL2.GL_LINE_LOOP);
		gl.glColor4fv(Theme.plotOutlineColor, 0);
			gl.glVertex2f(xPlotLeft,  yPlotTop);
			gl.glVertex2f(xPlotRight, yPlotTop);
			gl.glVertex2f(xPlotRight, yPlotBottom);
			gl.glVertex2f(xPlotLeft,  yPlotBottom);
		gl.glEnd();
		
	}

}
