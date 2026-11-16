import { useState } from 'react'

/** Muestra la foto del plato (servida desde S3); si no hay URL o no carga, muestra el emoji. */
export default function DishImage({ dish, className = '' }) {
  const [failedUrl, setFailedUrl] = useState(null)
  const showImage = dish.imageUrl && failedUrl !== dish.imageUrl
  return showImage
    ? <img className={`dish-photo ${className}`} src={dish.imageUrl} alt={dish.name} loading="lazy"
        onError={() => setFailedUrl(dish.imageUrl)} />
    : <span className={`dish-emoji ${className}`} aria-hidden="true">{dish.emoji}</span>
}
