package com.crashinvaders.texturepackergui.controllers.packing.processors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.crashinvaders.texturepackergui.services.model.PackModel;
import com.crashinvaders.texturepackergui.services.model.PngCompressionType;
import com.crashinvaders.texturepackergui.services.model.ProjectModel;
import com.crashinvaders.texturepackergui.services.model.compression.PngtasticCompressionModel;
import com.crashinvaders.texturepackergui.utils.packprocessing.PackProcessingNode;
import com.crashinvaders.texturepackergui.utils.packprocessing.PackProcessor;
import com.googlecode.pngtastic.core.PngImage;
import com.googlecode.pngtastic.core.PngOptimizer;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

public class PngtasticCompressingProcessor implements PackProcessor {
    private static final String LOG_LEVEL = "INFO";

    @Override
    public void processPackage(PackProcessingNode node) throws Exception {
        PackModel pack = node.getPack();
        ProjectModel project = node.getProject();

        if (!pack.getSettings().outputFormat.equals("png")) return;
        if (project.getPngCompression() == null || project.getPngCompression().getType() != PngCompressionType.PNGTASTIC) return;

        System.out.println("Pngtastic compression started");

        PngtasticCompressionModel compModel = (PngtasticCompressionModel)project.getPngCompression();
        PngOptimizer pngOptimizer = new PngOptimizer(LOG_LEVEL);

        // Compression section
        {
            TextureAtlas.TextureAtlasData atlasData = new TextureAtlas.TextureAtlasData(
                            Gdx.files.absolute(pack.getOutputDir()).child(pack.getCanonicalFilename()),
                            Gdx.files.absolute(pack.getOutputDir()), false);

            for (TextureAtlas.TextureAtlasData.Page page : atlasData.getPages()) {
                BufferedInputStream bis = null;
                try {
                    String fileName = page.textureFile.file().getAbsolutePath();
                    bis = new BufferedInputStream(new FileInputStream(fileName));
                    PngImage image = new PngImage(bis, LOG_LEVEL);
                    image.setFileName(fileName);
                    pngOptimizer.optimize(
                            image,
                            fileName,
                            compModel.isRemoveGamma(),
                            compModel.getLevel());
                } finally {
                    if (bis != null) {
                        bis.close();
                    }
                }
            }
        }

        // Compute compression rate for metadata
        {
            float compressionRate = 0f;
            for (PngOptimizer.OptimizerResult optimizerResult : pngOptimizer.getResults()) {
                float localCompressionRate = (optimizerResult.getOptimizedFileSize() - optimizerResult.getOriginalFileSize()) / (float) optimizerResult.getOriginalFileSize();
                compressionRate += localCompressionRate / pngOptimizer.getResults().size();
            }
            node.addMetadata(PackProcessingNode.META_COMPRESSION_RATE, compressionRate);
        }

        System.out.println("Pngtastic compression finished");
    }
}
