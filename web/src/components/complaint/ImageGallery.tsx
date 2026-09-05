import React, { useState } from 'react';
import { X, ZoomIn } from 'lucide-react';
import { Attachment } from '../../types';
import { motion, AnimatePresence } from 'framer-motion';

interface ImageGalleryProps {
  attachments: Attachment[];
}

const ImageGallery: React.FC<ImageGalleryProps> = ({ attachments }) => {
  const [lightboxImg, setLightboxImg] = useState<Attachment | null>(null);

  if (!attachments.length) {
    return (
      <div style={{ fontSize: '0.8rem', color: 'var(--text-3)', fontStyle: 'italic' }}>
        No attachments uploaded
      </div>
    );
  }

  return (
    <>
      <div className="image-gallery">
        {attachments.map((att) => (
          <div
            key={att.id}
            className="image-thumb"
            onClick={() => setLightboxImg(att)}
          >
            <img src={att.fileUrl} alt={att.fileName} loading="lazy" />
            <div className="image-thumb-overlay">
              <ZoomIn size={20} color="white" />
            </div>
          </div>
        ))}
      </div>

      <AnimatePresence>
        {lightboxImg && (
          <motion.div
            className="lightbox"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={() => setLightboxImg(null)}
          >
            <motion.div
              initial={{ scale: 0.85, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.85, opacity: 0 }}
              transition={{ type: 'spring', stiffness: 350, damping: 28 }}
              style={{ position: 'relative' }}
              onClick={(e) => e.stopPropagation()}
            >
              <img src={lightboxImg.fileUrl} alt={lightboxImg.fileName} />
              <button
                onClick={() => setLightboxImg(null)}
                style={{
                  position: 'absolute', top: -12, right: -12,
                  width: 32, height: 32, borderRadius: '50%',
                  background: 'var(--bg-elevated)', border: '1px solid var(--border)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  cursor: 'pointer', color: 'var(--text-1)',
                }}
              >
                <X size={14} />
              </button>
              <div
                style={{
                  position: 'absolute', bottom: -36,
                  left: 0, right: 0, textAlign: 'center',
                  fontSize: '0.8rem', color: 'var(--text-3)',
                }}
              >
                {lightboxImg.fileName}
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
};

export default ImageGallery;
