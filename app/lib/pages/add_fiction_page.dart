import 'package:app/models/fiction.dart';
import 'package:flutter/material.dart';

import '../services/api_service.dart';

class AddFictionPage extends StatefulWidget {
  const AddFictionPage({super.key});

  @override
  State<AddFictionPage> createState() => _AddFictionPageState();
}

class _AddFictionPageState extends State<AddFictionPage> {
  final TextEditingController urlController = TextEditingController();
  final ApiService apiService = ApiService();

  bool isLoading = false;
  String? errorMessage;

  @override
  void dispose() {
    urlController.dispose();
    super.dispose();
  }

  Future<void> addFiction() async {
    final url = urlController.text.trim();

    if (url.isEmpty) {
      setState(() {
        errorMessage = 'Please enter a fiction URL.';
      });
      return;
    }

    setState(() {
      isLoading = true;
      errorMessage = null;
    });

    try {
      final Fiction fiction = await apiService.addFiction(url);

      if (!mounted) {
        return;
      }

      Navigator.pop(context, fiction);
    } catch (e) {
      if (!mounted) {
        return;
      }

      setState(() {
        errorMessage = 'Failed to add fiction: $e';
      });
    } finally {
      if (mounted) {
        setState(() {
          isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Add Fiction'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Fiction URL'),
            const SizedBox(height: 8),
            TextField(
              controller: urlController,
              decoration: const InputDecoration(
                border: OutlineInputBorder(),
                hintText: 'https://www.royalroad.com/fiction/...',
              ),
              enabled: !isLoading,
            ),
            const SizedBox(height: 16),
            if (errorMessage != null)
              Text(
                errorMessage!,
                style: const TextStyle(color: Colors.red),
              ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: isLoading ? null : addFiction,
                child: isLoading
                    ? const CircularProgressIndicator()
                    : const Text('Add Fiction'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}