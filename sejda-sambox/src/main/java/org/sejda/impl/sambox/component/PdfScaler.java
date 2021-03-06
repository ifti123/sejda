/*
 * Created on 15 nov 2016
 * Copyright 2015 by Andrea Vacondio (andrea.vacondio@gmail.com).
 * Copyright 2017 by Edi Weissmann (edi.weissmann@gmail.com).
 *
 * This file is part of Sejda.
 *
 * Sejda is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Sejda is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Sejda.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.sejda.impl.sambox.component;

import static org.sejda.util.RequireUtils.requireNotNullArg;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Collections;

import org.sejda.model.exception.TaskIOException;
import org.sejda.model.scale.ScaleType;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.PDPageContentStream;
import org.sejda.sambox.pdmodel.PDPageContentStream.AppendMode;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.util.Matrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component capable of scaling pages or pages content
 * 
 * @author Andrea Vacondio
 * @author Eduard Weissmann
 *
 */
public class PdfScaler {
    private static final Logger LOG = LoggerFactory.getLogger(PdfScaler.class);

    private ScaleType type;

    public PdfScaler(ScaleType type) {
        requireNotNullArg(type, "Scale type cannot be null");
        this.type = type;
    }

    public void resizePages(PDDocument doc) throws TaskIOException {
        PDPage firstPage = doc.getPage(0);
        PDRectangle sizeOfFirstPage = firstPage.getCropBox();
        float targetWidth = Math.min(sizeOfFirstPage.getWidth(), sizeOfFirstPage.getHeight());
        resizePages(doc, doc.getPages(), targetWidth);
    }

    public void resizePages(PDDocument doc, Iterable<PDPage> pages, float targetWidth) throws TaskIOException {
        for(PDPage page : pages){
            PDRectangle cropBox = page.getCropBox();

            // handles landscape scenarios, where we'd like the scale to be calculated based on the width1/width2 ratio, not width1/height2
            double scale = targetWidth / Math.min(cropBox.getWidth(), cropBox.getHeight());

            LOG.debug("Scaling page from {} to {}, factor of {}", cropBox.getWidth(), targetWidth, scale);

            try (PDPageContentStream contentStream = new PDPageContentStream(doc, page, AppendMode.PREPEND, true)) {
                Matrix matrix = getMatrix(scale, page.getCropBox(), page.getCropBox());
                contentStream.transform(matrix);
                if (ScaleType.PAGE == type) {
                    scalePageBoxes(scale, page);
                } else {
                    scaleContentBoxes(scale, page);
                }

            } catch (IOException e) {
                throw new TaskIOException("An error occurred writing scaling the page.", e);
            }
        }
    }

    public void scale(PDDocument doc, double scale) throws TaskIOException {
        scale(doc, doc.getPages(), scale);
    }

    public void scale(PDDocument doc, PDPage page, double scale) throws TaskIOException {
        scale(doc, Collections.singletonList(page), scale);
    }

    public void scale(PDDocument doc, Iterable<PDPage> pages, double scale) throws TaskIOException {
        if (scale != 1) {
            for (PDPage page : pages) {
                try (PDPageContentStream contentStream = new PDPageContentStream(doc, page, AppendMode.PREPEND, true)) {
                    Matrix matrix = getMatrix(scale, page.getCropBox(), page.getCropBox());
                    contentStream.transform(matrix);
                    if (ScaleType.PAGE == type) {
                        scalePageBoxes(scale, page);
                    } else {
                        scaleContentBoxes(scale, page);
                    }

                } catch (IOException e) {
                    throw new TaskIOException("An error occurred writing scaling the page.", e);
                }
            }
        }
    }

    private Matrix getMatrix(double scale, PDRectangle crop, PDRectangle toScale) {
        if (ScaleType.CONTENT == type) {
            AffineTransform transform = AffineTransform.getTranslateInstance(
                    (crop.getWidth() - (toScale.getWidth() * scale)) / 2,
                    (crop.getHeight() - (toScale.getHeight() * scale)) / 2);
            transform.scale(scale, scale);
            return new Matrix(transform);
        }
        return new Matrix(AffineTransform.getScaleInstance(scale, scale));
    }

    private void scaleContentBoxes(double scale, PDPage page) {
        PDRectangle cropBox = page.getCropBox();
        // we adjust art and bleed same as Acrobat does
        if (scale > 1) {
            page.setBleedBox(cropBox);
            page.setTrimBox(cropBox);
        } else {
            page.setBleedBox(new PDRectangle(page.getBleedBox()
                    .transform(getMatrix(scale, page.getCropBox(), page.getBleedBox())).getBounds2D()));
            page.setTrimBox(new PDRectangle(
                    page.getTrimBox().transform(getMatrix(scale, page.getCropBox(), page.getTrimBox())).getBounds2D()));
        }
        Rectangle2D newArt = page.getArtBox().transform(getMatrix(scale, page.getCropBox(), page.getArtBox()))
                .getBounds2D();
        if (newArt.getX() < cropBox.getLowerLeftX() || newArt.getY() < cropBox.getLowerLeftX()) {
            // we overlow the cropbox
            page.setArtBox(page.getCropBox());
        } else {
            page.setArtBox(new PDRectangle(newArt));
        }
    }

    private void scalePageBoxes(double scale, PDPage page) {
        page.setArtBox(new PDRectangle(
                page.getArtBox().transform(getMatrix(scale, page.getCropBox(), page.getArtBox())).getBounds2D()));
        page.setBleedBox(new PDRectangle(
                page.getBleedBox().transform(getMatrix(scale, page.getCropBox(), page.getBleedBox())).getBounds2D()));
        page.setTrimBox(new PDRectangle(
                page.getTrimBox().transform(getMatrix(scale, page.getCropBox(), page.getTrimBox())).getBounds2D()));
        page.setCropBox(new PDRectangle(
                page.getCropBox().transform(getMatrix(scale, page.getCropBox(), page.getCropBox())).getBounds2D()));
        page.setMediaBox(new PDRectangle(
                page.getMediaBox().transform(getMatrix(scale, page.getMediaBox(), page.getMediaBox())).getBounds2D()));
    }

}
