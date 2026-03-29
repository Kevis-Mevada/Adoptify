import glob
import re

files = glob.glob('../frontend/**/*.html', recursive=True)

new_config = """<link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
    
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    fontFamily: { sans: ['Inter', 'sans-serif'] },
                    colors: {
                        teal: { 50: '#f8fafc', 100: '#f1f5f9', 200: '#e2e8f0', 300: '#cbd5e1', 400: '#94a3b8', 500: '#334155', 600: '#1e293b', 700: '#0f172a', 800: '#020617', 900: '#black' },
                        indigo: { 50: '#f9fafb', 100: '#f3f4f6', 200: '#e5e7eb', 300: '#d1d5db', 400: '#9ca3af', 500: '#6b7280', 600: '#4b5563', 700: '#374151', 800: '#1f2937', 900: '#111827' }
                    },
                    borderRadius: {
                        'none': '0', 'sm': '0', 'DEFAULT': '0', 'md': '0', 'lg': '0', 'xl': '0', '2xl': '0', '3xl': '0',
                        'full': '4px', '[1.5rem]': '0', '[2rem]': '0', '[2.5rem]': '0'
                    },
                    boxShadow: {
                        'sm': 'none', 'md': 'none', 'lg': 'none', 'xl': 'none', '2xl': 'none', '[0_8px_30px_rgb(0,0,0,0.03)]': 'none'
                    },
                    animation: {
                        'fade-in': 'none', 'slide-up': 'none', 'slide-down': 'none', 'bounce': 'none', 'pulse': 'none', 'blob': 'none', 'pulse-slow': 'none'
                    },
                    transitionDuration: {
                        '300': '0ms', '500': '0ms', '700': '0ms', '1000': '0ms', 'DEFAULT': '50ms'
                    }
                }
            }
        }
    </script>"""

for fpath in files:
    with open(fpath, 'r') as f:
        content = f.read()

    start_pattern = r'<link href="https://fonts\.googleapis\.com/css2\?family=Outfit.*?<script>\s*tailwind\.config.*?\}\s*\}\s*\}\s*</script>'
    
    new_content = re.sub(start_pattern, new_config, content, flags=re.DOTALL)
    
    if new_content != content:
        with open(fpath, 'w') as f:
            f.write(new_content)
        print(f"Updated {fpath}")

