import 'package:app/pages/add_fiction_page.dart';
import 'package:app/pages/chapter_page.dart';
import 'package:app/pages/finished_fictions_page.dart';
import 'package:app/services/api_service.dart';
import 'package:app/themes/theme.dart';
import 'package:app/widgets/ficCard.dart';
import 'package:flutter/material.dart';
import 'package:app/models/fiction.dart';


final ValueNotifier<ThemeMode> themeMode = ValueNotifier(ThemeMode.light);

void main() {
  runApp(const FicScraperApp());
}

class FicScraperApp extends StatelessWidget {
  const FicScraperApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder(
      valueListenable: themeMode, 
      builder: (context, mode, child) {  
      return MaterialApp(
        theme: lightMode,
        darkTheme: darkMode,
        themeMode: mode,
        home: const FictionPage(),
      );

      }); 
  }
}

class FictionPage extends StatefulWidget {
  const FictionPage({super.key});

  @override
  State<FictionPage> createState() => _FictionPageState();
}

class _FictionPageState extends State<FictionPage> {
  final ApiService apiService = ApiService();

  late Future<List<Fiction>> fictions;

  @override
  void initState() {
    super.initState();
    fictions = apiService.getFictions();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Row(
          children: [
            PopupMenuButton<String>(
              onSelected: (value) async {
                if (value == 'add') {
                  final Fiction? addedFiction = await Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) => const AddFictionPage(),
                    ),
                  );

                  if (addedFiction != null) {
                    setState(() {
                      fictions = apiService.getFictions();
                    });
                  }
                } 
                if (value == 'finished') {
                  Navigator.push(
                    context, 
                    MaterialPageRoute(builder: (context) => const FinishedFictionsPage()
                   ),
                  );
                }
              },
              itemBuilder: (context) => const [
                PopupMenuItem(
                  value: 'library',
                  child: Text('Library'),
                ),
                PopupMenuItem(
                  value: 'finished',
                  child: Text('Finished Fictions'),
                ),
                PopupMenuDivider(),
                PopupMenuItem(
                  value: 'add',
                  child: Text('Add Fiction'),
                ),
                PopupMenuItem(
                  value: 'remove',
                  child: Text('Remove Fiction'),
                ),
                PopupMenuItem(
                  value: 'finish',
                  child: Text('Mark as Finished'),
                ),
                PopupMenuItem(
                  value: 'refresh',
                  child: Text('Refresh Fiction'),
                ),
              ],
              icon: const Icon(Icons.menu),
            ),
            const Text(
              'FicScraper',
            ),
            ValueListenableBuilder<ThemeMode>(
              valueListenable: themeMode,
              builder: (context, mode, child) {
                return IconButton(
                  onPressed: () {
                    themeMode.value =
                        mode == ThemeMode.light
                            ? ThemeMode.dark
                            : ThemeMode.light;
                  },
                  icon: Icon(
                    mode == ThemeMode.light
                        ? Icons.dark_mode
                        : Icons.light_mode,
                  ),
                );
              },
            ),
          ],
        ),
        backgroundColor: Colors.grey,
      ),
      body: FutureBuilder<List<Fiction>>(
        future: fictions,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(
              child: CircularProgressIndicator(),
            );
          }

          if (snapshot.hasError) {
            return Center(
              child: Text('Error: ${snapshot.error}'),
            );
          }

          final fictions = snapshot.data ?? [];

          if (fictions.isEmpty) {
            return const Center(
              child: Text('No fictions found.'),
            );
          }
          return LayoutBuilder(
            builder: ((context, constraints) {
            const minCardWidth = 220.0;
            const spacing = 10.0;
            final columns = ((constraints.maxWidth + spacing) / (minCardWidth + spacing))
              .floor()
              .clamp(1, 20);   
            return GridView.builder(
              padding: const EdgeInsets.all(10),
              itemCount: fictions.length,
              gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: columns,
                crossAxisSpacing: spacing,
                mainAxisSpacing: spacing,
              ),
              itemBuilder: (context, index) {
                return FicCard(
                  fiction: fictions[index],
                  onTap: () async {
                    print("Pressed ${fictions[index].title}");
                    final updatedFiction = await Navigator.push<Fiction>(
                      context,
                      MaterialPageRoute(
                        builder: ((context) => ChapterPage(
                          fiction: fictions[index]))
                      )
                    );
                    if (updatedFiction != null) {
                      setState(() {
                        this.fictions = this.fictions.then((currentFictions) {
                          return currentFictions.map((fiction) {
                            return fiction.id == updatedFiction.id 
                              ? updatedFiction
                              : fiction;
                          }).toList();
                        });
                      });
                    }
                  },
                  onFinish: () async {
                    try {
                      await apiService.finishFiction(fictions[index].id);
                      setState(() {
                        this.fictions = apiService.getFictions();
                      });
                    } catch (e) {
                      print(e);
                    }
                  },
                  onRemove: () async {
                    try {
                      await apiService.removeFiction(fictions[index].id);

                      setState(() {
                        this.fictions = apiService.getFictions();
                      });
                    } catch (e) {
                      print(e);
                    }
                  },
                );
              },
            );
          }));
        },
      ),
    );
  }
}


