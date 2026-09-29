import 'package:app/models/chapter.dart';
import 'package:app/pages/add_fiction_page.dart';
import 'package:app/pages/finished_fictions_page.dart';
import 'package:app/pages/reader_page.dart';
import 'package:app/services/api_service.dart';
import 'package:flutter/material.dart';
import 'package:app/models/fiction.dart';

void main() {
  runApp(const FicScraperApp());
}

class FicScraperApp extends StatelessWidget {
  const FicScraperApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: const FictionPage(),
    );
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
              style: TextStyle(color: Colors.black),
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

class FicCard extends StatelessWidget {
  final Fiction fiction;
  final VoidCallback onTap;
  final VoidCallback? onFinish;
  final VoidCallback onRemove;

  const FicCard({
    required this.fiction,
    required this.onTap,
    this.onFinish,
    required this.onRemove,

    super.key,
  });

  @override
  Widget build(BuildContext context) {
    final unread = fiction.chapAmount - fiction.readerProgress;
    return LayoutBuilder(
      builder: ((context, constraints) {
        final fontSize = constraints.maxWidth * 0.06;      
      return InkWell(
        onTap: onTap,
        child: Container(
          width: 300,
          padding: const EdgeInsets.all(20),
          margin: const EdgeInsets.only(right: 10),
          color: Colors.amber,
          child: Stack(
            children: [
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(fiction.title, 
                  style: 
                    TextStyle(
                      fontSize: fontSize),
                ),
                Text(fiction.author,
                style: 
                    TextStyle(
                      fontSize: fontSize),
                ),
                Text(fiction.site, 
                style: 
                    TextStyle(
                      fontSize: fontSize),
                ),
                Text('${fiction.chapAmount} chapters', 
                style: 
                    TextStyle(
                      fontSize: fontSize),
                ),
                Text('${fiction.wordCount} words', 
                style: 
                    TextStyle(
                      fontSize: fontSize),
                ),
                Text('${unread} chapter(s)', style: 
                    TextStyle(
                      fontSize: fontSize)
                ),
              ],
            ),
            Positioned(
              top: 0,
              right: 0,
              child: PopupMenuButton<String>(
                onSelected: (value) {
                  if (value == "finish") {
                    onFinish?.call();
                  } 
                  if (value == "remove") {
                    onRemove();
                  }
                },
                itemBuilder: (context) => const [
                  PopupMenuItem(
                    value: "finish",
                    child: Text("Finish")
                    ),
                  PopupMenuItem(
                    value: "remove",
                    child: Text("Remove")
                    ),
                  ],
                )
              )
            ]
          )
        ),
      );
      }));
  }
}

class ChapterPage extends StatefulWidget {
  final Fiction fiction;

  const ChapterPage({
    required this.fiction,
    super.key
  }); 

  @override
  State<ChapterPage> createState() => _ChapterPageState();
}

class _ChapterPageState extends State<ChapterPage> {
  late Fiction fiction;
  late Future<List<Chapter>> chapters;
  bool order = false;
  @override
  void initState() {
    super.initState();
    fiction = widget.fiction;
  }

  Future<void> refreshFiction() async {
    final updatedFiction = 
      await ApiService().getFiction(widget.fiction.id);
    
    setState(() {
      fiction = updatedFiction;
    });
  }


  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) {
        if (!didPop) {
          Navigator.pop(context, fiction);
        }
      },
      child: Scaffold(
      appBar: AppBar(
        title: Row(
          children: [
            IconButton(
              onPressed: () {
                setState(() {
                  order = !order;
                });
              }, 
              icon: Icon(Icons.sort)
              ),
            const Text("Chapters"),
          ],
        ),
        
      ),
      body: FutureBuilder<List<Chapter>>(
        future: chapters = ApiService().getChapters(widget.fiction.id), 
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

          final chapters = snapshot.data ?? [];

          if (chapters.isEmpty) {
            return const Center(
              child: Text('No chapters found.'),
            );
          }

          final displayedChapters = order ? chapters.reversed.toList() : chapters;

          return ListView.builder(
            itemCount: displayedChapters.length,
            itemBuilder: (context, index) {
              final chapter = displayedChapters[index];
              final isUnread = chapter.isUnread(fiction.readerProgress);  
              
              return ListTile(
                title: Text(chapter.title),
                subtitle: Text("Chapter ${chapter.chapterNumber}"),
                tileColor: isUnread ? Colors.blueGrey : null,
                onTap: () async {
                  final originalIndex = chapters.indexOf(chapter);

                  await Navigator.push(context, MaterialPageRoute(
                    builder: (context) => ReaderPage(
                      fictionId: widget.fiction.id,
                      chapters: chapters,
                      currentIndex: originalIndex,
                      )
                    )
                  );
                  await refreshFiction();
                },
              );
            },
          );
        },
      ),
    )); 
  }
}
