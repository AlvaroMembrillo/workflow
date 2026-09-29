# Cabeceras de seguridad de la web. Van en un fichero aparte porque nginx no hereda add_header en las
# location que definen las suyas propias.
#
# CSP: solo se ejecuta JavaScript servido por la propia web (sin scripts en línea ni de terceros).
# Los estilos admiten 'unsafe-inline' porque Angular inserta los de cada componente al ejecutarse.
add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'" always;
add_header X-Content-Type-Options "nosniff" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;
