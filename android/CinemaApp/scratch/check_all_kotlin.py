import os, re

kotlin_dir = r"c:\Users\Sys\Desktop\Cinemas APP New\android\CinemaApp\app\src\main\java"

all_files = []
total_issues = 0

print("=== Scanning ALL 60 Kotlin Files in Android App ===")
for root, dirs, files in os.walk(kotlin_dir):
    for f in files:
        if f.endswith('.kt'):
            path = os.path.join(root, f)
            all_files.append(path)
            with open(path, 'r', encoding='utf-8') as file:
                lines = file.readlines()
                file_issues = []
                content = "".join(lines)

                # Check for common unresolved symbols
                if 'sp' in content and 'import androidx.compose.ui.unit.sp' not in content and 'import androidx.compose.ui.unit.*' not in content:
                    # check if sp is used as a unit
                    if re.search(r'\b\d+(\.\d+)?\.sp\b', content):
                        file_issues.append("Missing import androidx.compose.ui.unit.sp")
                
                if 'Icons.Default.Movie' in content and 'import androidx.compose.material.icons.filled.Movie' not in content and 'import androidx.compose.material.icons.filled.*' not in content:
                    file_issues.append("Missing import androidx.compose.material.icons.filled.Movie")

                if 'Icons.Default.Tv' in content:
                    file_issues.append("Unresolved reference Icons.Default.Tv")

                if 'CinemaTypography.button' in content:
                    file_issues.append("Unresolved reference CinemaTypography.button")

                if file_issues:
                    print(f"❌ {f}:")
                    for issue in file_issues:
                        print(f"   - {issue}")
                    total_issues += len(file_issues)

print(f"\nTotal files scanned: {len(all_files)}")
print(f"Total potential issues found: {total_issues}")
