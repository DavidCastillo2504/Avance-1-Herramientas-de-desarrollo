-- URL pública de la imagen del plato (bucket S3). Si es NULL, el frontend muestra el emoji.
ALTER TABLE menu_items ADD COLUMN image_url VARCHAR(500);
