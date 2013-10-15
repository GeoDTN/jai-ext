package it.geosolutions.jaiext.crop;

import it.geosolutions.jaiext.mosaic.ImageMosaicBean;
import it.geosolutions.jaiext.mosaic.MosaicOpImage;
import it.geosolutions.jaiext.range.Range;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.operator.MosaicDescriptor;

import com.sun.media.jai.opimage.RIFUtil;

/**
 * The image factory for the Crop operator.
 * 
 * @author Andrea Aime
 * @since 2.7.2
 *
 * @source $URL$
 */
public class CropCRIF implements RenderedImageFactory {

    public CropCRIF() {
    }

    /**
     * Creates a new instance of {@link CropOpImage} in the rendered layer.
     * 
     * @param paramBlock
     *            parameter block with parameters minx, miny, width height
     * 
     * @param renderHints
     *            optional rendering hints which may be used to pass down a tile scheduler and tile
     *            cache
     */
    public RenderedImage create(ParameterBlock paramBlock, RenderingHints renderingHints) {
        RenderedImage image = (RenderedImage) paramBlock.getSource(0);
        float x = paramBlock.getFloatParameter(CropDescriptor.X_ARG);
        float y = paramBlock.getFloatParameter(CropDescriptor.Y_ARG);
        float width = paramBlock.getFloatParameter(CropDescriptor.WIDTH_ARG);
        float height = paramBlock.getFloatParameter(CropDescriptor.HEIGHT_ARG);
        ROI roi = (ROI) paramBlock.getObjectParameter(CropDescriptor.ROI_ARG);
        Range noData = (Range) paramBlock.getObjectParameter(CropDescriptor.NO_DATA_ARG);
        double[] destNoData = (double[]) paramBlock.getObjectParameter(CropDescriptor.DEST_NO_DATA_ARG);
        
        // only leave tile cache and tile scheduler (we can't instantiate directly RenderingHints
        // as it won't allow for a null tile cache, even if the rest of JAI handles that peachy
        Map<Key, Object> tmp = new HashMap<RenderingHints.Key, Object>();
        for (Object key : renderingHints.keySet()) {
            if (key == JAI.KEY_TILE_CACHE || key == JAI.KEY_TILE_SCHEDULER) {
                tmp.put((Key) key, renderingHints.get(key));
            }
        }
        RenderingHints local = new RenderingHints(tmp);
        // selection of the layout
        ImageLayout layout = RIFUtil.getImageLayoutHint(local);
        // Creation of a Rectangle object containing the starting bounds
        Rectangle bounds = new Rectangle2D.Float(x, y, width, height).getBounds();
        // Initialization of the final bounds
        Rectangle finalBounds = bounds;
        // If roi is present the final bounds are intersected with the ROI object
        if(roi != null){
            Rectangle roiBounds = roi.getBounds();   
            
            if(finalBounds.contains(roiBounds)){
                finalBounds = roiBounds;
            }else{
                finalBounds.intersection(roiBounds);
            }  
        }
        // The final bounds coordinates are taken
        x = (float) finalBounds.getMinX();
        y = (float) finalBounds.getMinY();
        width = (float) finalBounds.getWidth();
        height = (float) finalBounds.getHeight();
        
        // If noData are present, the MosaicOpImage is used instead of the crop
        if(noData!=null){
            // The calculated bounds are taken as an input roi
            roi = new ROIShape(finalBounds);
            // The source image is taken as a list of data
            List<RenderedImage> listSrc = new Vector<RenderedImage>();            
            listSrc.add(image);
            // The related ImageMosaicBean object is created
            ImageMosaicBean[] bean = new ImageMosaicBean[1];            
            bean[0] = new ImageMosaicBean();
            // Setting of the parameters
            bean[0].setImage(image);
            bean[0].setImageRoi(roi);
            bean[0].setSourceNoData(noData);
            // Mosaic operation
            image = new MosaicOpImage(listSrc, layout, local, bean, MosaicDescriptor.MOSAIC_TYPE_OVERLAY, destNoData);
        }
        
        // If noData are not present, then the crop operation is performed
        return new CropOpImage(image, x, y, width, height, local);
    }
}
